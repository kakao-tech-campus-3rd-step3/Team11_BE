package com.pnu.momeet.domain.evaluation.service;

import com.pnu.momeet.common.mapper.facade.EvaluatableProfileMapper;
import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;
import com.pnu.momeet.domain.meetup.dto.response.UnEvaluatedMeetupDto;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.service.ParticipantDomainService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationQueryService {

    private final EvaluationRepository evaluationRepository;
    private final ParticipantDomainService participantService;
    private final ProfileService profileSerivice;
    private final MeetupDomainService meetupService;

    public long calculateUnEvaluatedCount(Meetup meetup, UUID evaluatorProfileId) {
        long evaluatedCount = evaluationRepository.countByMeetupIdAndEvaluatorProfileId(
            meetup.getId(), evaluatorProfileId
        );

        return meetup.getParticipantCount() - 1 - evaluatedCount;
    }

    public List<EvaluatableProfileResponse> getEvaluatableUsers(UUID meetupId, UUID evaluatorMemberId) {
        Profile evaluatorProfile = profileSerivice.getProfileEntityByMemberId(evaluatorMemberId);
        // 1. 모임 참가자 조회
        List<ParticipantResponse> participants = participantService.getParticipantsByMeetupId(meetupId);

        // 2. 자기 자신 제외
        List<ParticipantResponse> targetParticipants = participants.stream()
            .filter(p -> !p.profile().id().equals(evaluatorProfile.getId()))
            .toList();

        // 3. 기존 평가 기록 불러오기
        Map<UUID, Evaluation> existingEvaluations = evaluationRepository
            .findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorProfile.getId())
            .stream()
            .collect(Collectors.toMap(Evaluation::getTargetProfileId, e -> e));

        // 4. 응답 변환
        return targetParticipants.stream()
            .map(participantResponse -> EvaluatableProfileMapper
                .toEvaluatableProfileResponse(
                    participantResponse,
                    existingEvaluations.get(participantResponse.profile().id())
                ))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<UnEvaluatedMeetupDto> getUnEvaluatedMeetups(UUID memberId, Pageable pageable) {
        Profile me = profileSerivice.getProfileEntityByMemberId(memberId);

        Page<Meetup> meetups = meetupService.findEndedMeetupsByProfileId(me.getId(), pageable);

        return meetups.map(meetup -> {
            long unEvaluatedCount = calculateUnEvaluatedCount(meetup, me.getId());
            return MeetupEntityMapper.unEvaluatedMeetupDto(meetup, unEvaluatedCount);
        });
    }
}
