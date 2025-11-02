package com.pnu.momeet.domain.evaluation.service;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationEntityService {

    private final EvaluationRepository evaluationRepository;

    @Transactional(readOnly = true)
    public boolean existsByMeetupAndPair(UUID meetupId, UUID evaluatorPid, UUID targetPid) {
        boolean exists = evaluationRepository.existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
            meetupId, evaluatorPid, targetPid
        );
        log.debug("평가 중복 여부 조회. meetupId={}, evaluatorPid={}, targetPid={}, exists={}",
            meetupId, evaluatorPid, targetPid, exists);
        return exists;
    }

    @Transactional(readOnly = true)
    public Optional<Evaluation> getLastByPair(UUID evaluatorPid, UUID targetPid) {
        Optional<Evaluation> found = evaluationRepository
            .findTopByEvaluatorProfileIdAndTargetProfileIdOrderByCreatedAtDesc(evaluatorPid, targetPid);
        log.debug("최근 evaluator→target 평가 조회. evaluatorPid={}, targetPid={}, exists={}",
            evaluatorPid, targetPid, found.isPresent());
        return found;
    }

    @Transactional(readOnly = true)
    public Map<UUID, LocalDateTime> getLastCreatedAtByTargets(UUID evaluatorPid, Set<UUID> targetPids) {
        log.debug("모임 참가자들에 대해 최근 생성한 평가 시각 조회. evaluatorPid={}, targetPids={}", evaluatorPid, targetPids);
        if (targetPids == null || targetPids.isEmpty()) return Map.of();
        return evaluationRepository.findLastCreatedAtByTargets(evaluatorPid, targetPids);
    }

    @Transactional(readOnly = true)
    public boolean existsByMeetupTargetIpAfter(UUID meetupId, UUID targetPid, String ipHash, LocalDateTime after) {
        boolean exists = evaluationRepository
            .existsByMeetupIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(meetupId, targetPid, ipHash, after);
        log.debug("동일 IP 쿨타임 내 중복 여부. meetupId={}, targetPid={}, after={}, exists={}",
            meetupId, targetPid, after, exists);
        return exists;
    }

    @Transactional(readOnly = true)
    public List<Evaluation> getByMeetupAndEvaluator(UUID meetupId, UUID evaluatorPid) {
        List<Evaluation> list = evaluationRepository.findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorPid);
        log.debug("모임별 evaluator의 평가 목록 조회. meetupId={}, evaluatorPid={}, size={}",
            meetupId, evaluatorPid, list.size());
        return list;
    }

    @Transactional
    public Evaluation save(Evaluation toSave) {
        log.debug("평가 저장 시도. meetupId={}, evaluatorPid={}, targetPid={}, rating={}, ipHashLen={}",
            toSave.getMeetupId(), toSave.getEvaluatorProfileId(), toSave.getTargetProfileId(),
            toSave.getRating(), (toSave.getIpHash() == null ? 0 : toSave.getIpHash().length()));
        Evaluation saved = evaluationRepository.save(toSave);
        log.debug("평가 저장 성공. id={}, meetupId={}, evaluatorPid={}, targetPid={}",
            saved.getId(), saved.getMeetupId(), saved.getEvaluatorProfileId(), saved.getTargetProfileId());
        return saved;
    }

    @Transactional
    public void deleteById(UUID evaluationId) {
        log.debug("평가 삭제 시도. id={}", evaluationId);
        if (!evaluationRepository.existsById(evaluationId)) {
            log.info("존재하지 않는 평가 삭제 시도. id={}", evaluationId);
            throw new NoSuchElementException("삭제할 평가가 존재하지 않습니다. id=" + evaluationId);
        }
        evaluationRepository.deleteById(evaluationId);
        log.debug("평가 삭제 성공. id={}", evaluationId);
    }

    public Set<UUID> getMeetupIdsEvaluatedBy(UUID evaluatorId, List<UUID> meetupIds) {
        return evaluationRepository.findEvaluatedMeetupIds(evaluatorId, meetupIds);
    }
}
