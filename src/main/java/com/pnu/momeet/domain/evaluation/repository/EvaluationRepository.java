package com.pnu.momeet.domain.evaluation.repository;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID>, EvaluationDslRepository {

    boolean existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
        UUID meetupId,
        UUID evaluationProfileId,
        UUID targetProfileId
    );

    Optional<Evaluation> findTopByEvaluatorProfileIdAndTargetProfileIdOrderByCreatedAtDesc(
        UUID evaluationProfileId,
        UUID targetProfileId
    );

    boolean existsByMeetupIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
        UUID meetupId,
        UUID targetProfileId,
        String ipHash,
        LocalDateTime createdAt
    );

    long countByMeetupIdAndEvaluatorProfileId(UUID meetupId, UUID evaluatorProfileId);

    List<Evaluation> findByMeetupIdAndEvaluatorProfileId(UUID meetupId, UUID evaluatorProfileId);
}