package com.pnu.momeet.domain.evaluation.repository;

import com.pnu.momeet.domain.evaluation.entity.QEvaluation;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EvaluationDslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Set<UUID> findEvaluatedMeetupIds(UUID evaluatorId, Collection<UUID> meetupIds) {
        if (meetupIds == null || meetupIds.isEmpty()) return Collections.emptySet();

        QEvaluation e = QEvaluation.evaluation;

        List<UUID> rows = jpaQueryFactory
            .select(e.meetupId)
            .from(e)
            .where(e.evaluatorProfileId.eq(evaluatorId)
                .and(e.meetupId.in(meetupIds)))
            .groupBy(e.meetupId)
            .fetch();

        return new HashSet<>(rows);
    }
}
