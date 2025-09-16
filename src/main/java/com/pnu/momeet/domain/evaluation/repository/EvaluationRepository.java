package com.pnu.momeet.domain.evaluation.repository;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

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
}