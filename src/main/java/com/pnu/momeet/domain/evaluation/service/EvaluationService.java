package com.pnu.momeet.domain.evaluation.service;

import static com.pnu.momeet.domain.evaluation.enums.Rating.DISLIKE;
import static com.pnu.momeet.domain.evaluation.enums.Rating.LIKE;

import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateRequest;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;
import com.pnu.momeet.domain.evaluation.service.mapper.EvaluationEntityMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ProfileService profileService;

    private static final Duration EVALUATION_COOLTIME = Duration.ofHours(24);

    @Transactional
    public EvaluationResponse createEvaluation(
        UUID evaluatorMemberId,
        EvaluationCreateRequest request,
        String ipHash
    ) {
        Profile evaluatorProfile = profileService.getProfileEntityByMemberId(evaluatorMemberId);
        Profile targetProfile = profileService.getProfileEntityByProfileId(request.targetProfileId());

        // 모임 단위 중복 평가 방지
        if (evaluationRepository.existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
            request.meetupId(), evaluatorProfile.getId(), targetProfile.getId())) {
            throw new IllegalStateException("이미 평가한 사용자입니다.");
        }

        // 동일 evaluator → target 에 대한 쿨타임 검증
        evaluationRepository.findTopByEvaluatorProfileIdAndTargetProfileIdOrderByCreatedAtDesc(
                evaluatorProfile.getId(),
                targetProfile.getId()
            ).filter(lastEval -> lastEval
                .getCreatedAt()
                .isAfter(LocalDateTime.now().minus(EVALUATION_COOLTIME)))
            .ifPresent(e -> {
                throw new IllegalStateException("동일 사용자에 대한 평가는 하루에 한 번만 가능합니다.");
            });

        // 동일 위치 ( IP 해시 ) 다계정 평가 검증
        boolean existsBySameIp = evaluationRepository
            .existsByMeetupIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
                request.meetupId(),
                targetProfile.getId(),
                ipHash,
                LocalDateTime.now().minus(EVALUATION_COOLTIME)
            );
        if (existsBySameIp) {
            throw new IllegalStateException("동일 위치에서 이미 해당 사용자에 대한 평가가 등록되었습니다.");
        }

        switch (request.rating()) {
            case LIKE:
                targetProfile.increaseLikes();
                break;
            case DISLIKE:
                targetProfile.increaseDislikes();
                break;
            default:
                throw new IllegalArgumentException("유효하지 않은 평가입니다.");
        }

        Evaluation newEvaluation = Evaluation.create(
            request.meetupId(),
            evaluatorProfile.getId(),
            targetProfile.getId(),
            request.rating(),
            ipHash
        );

        return EvaluationEntityMapper.toResponseDto(evaluationRepository.save(newEvaluation));
    }
}
