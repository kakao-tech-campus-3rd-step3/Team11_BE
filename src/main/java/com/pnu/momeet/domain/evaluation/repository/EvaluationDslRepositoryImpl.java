package com.pnu.momeet.domain.evaluation.repository;

import com.pnu.momeet.domain.evaluation.entity.QEvaluation;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EvaluationDslRepositoryImpl implements EvaluationDslRepository {

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

    public Map<UUID, LocalDateTime> findLastCreatedAtByTargets(UUID evaluatorId, Collection<UUID> targetIds) {
        QEvaluation e = QEvaluation.evaluation;

        if (evaluatorId == null || targetIds == null || targetIds.isEmpty()) {
            return Map.of();
        }

        DateTimeExpression<LocalDateTime> lastExpr = e.createdAt.max();

        List<Tuple> rows = jpaQueryFactory
            .select(e.targetProfileId, lastExpr)
            .from(e)
            .where(
                e.evaluatorProfileId.eq(evaluatorId),
                e.targetProfileId.in(targetIds)
            )
            .groupBy(e.targetProfileId)
            .fetch();

        return rows.stream().collect(Collectors.toMap(
            t -> t.get(e.targetProfileId),
            t -> t.get(lastExpr)
        ));
    }
}
