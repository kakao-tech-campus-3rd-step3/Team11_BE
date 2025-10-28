package com.pnu.momeet.domain.evaluation.repository;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface EvaluationDslRepository {
    Set<UUID> findEvaluatedMeetupIds(UUID evaluatorId, Collection<UUID> meetupIds);
}
