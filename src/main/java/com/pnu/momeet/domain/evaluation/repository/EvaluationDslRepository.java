package com.pnu.momeet.domain.evaluation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface EvaluationDslRepository {
    Set<UUID> findEvaluatedMeetupIds(UUID evaluatorId, Collection<UUID> meetupIds);
    Map<UUID, LocalDateTime> findLastCreatedAtByTargets(UUID evaluatorId, Collection<UUID> targetIds);
}
