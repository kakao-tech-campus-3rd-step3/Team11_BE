package com.pnu.momeet.domain.participant.repository;

import com.pnu.momeet.domain.block.entity.QUserBlock;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.entity.QParticipant;
import com.pnu.momeet.domain.profile.entity.QProfile;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantDslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Participant> findAllVisibleByMeetupId(UUID meetupId, UUID viewerMemberId) {
        QParticipant p = QParticipant.participant;
        QProfile prof = QProfile.profile;
        QUserBlock ub = QUserBlock.userBlock;

        BooleanExpression viewerBlocksTarget =
            ub.blockerId.eq(viewerMemberId).and(ub.blockedId.eq(prof.memberId));
        BooleanExpression targetBlocksViewer =
            ub.blockerId.eq(prof.memberId).and(ub.blockedId.eq(viewerMemberId));
        BooleanExpression blockedEither = viewerBlocksTarget.or(targetBlocksViewer);

        // 상호 차단이 없는 경우만 조회
        BooleanExpression notBlocked = JPAExpressions.selectOne()
            .from(ub)
            .where(blockedEither)
            .notExists();

        return jpaQueryFactory.selectFrom(p)
            .join(p.profile, prof).fetchJoin()
            .where(p.meetup.id.eq(meetupId).and(notBlocked))
            .orderBy(p.id.asc())
            .fetch();
    }
}
