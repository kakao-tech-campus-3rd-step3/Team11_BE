package com.pnu.momeet.domain.meetup.repository;

import com.pnu.momeet.domain.block.entity.QUserBlock;
import com.pnu.momeet.domain.evaluation.entity.QEvaluation;
import com.pnu.momeet.domain.meetup.entity.QMeetupHashTag;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.participant.entity.QParticipant;
import com.pnu.momeet.domain.sigungu.entity.QSigungu;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.entity.QMeetup;
import com.pnu.momeet.domain.profile.entity.QProfile;
import java.time.LocalDateTime;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MeetupDslRepositoryImpl implements MeetupDslRepository {
    private static final int EVALUATION_VALID_DAYS = 3;
    private final JPAQueryFactory jpaQueryFactory;

    public MeetupDslRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    private BooleanExpression distanceWithin(Point location, double radius) {
        return Expressions.booleanTemplate(
                "function('ST_DWithin', {0}, {1}, {2}) = true",
                QMeetup.meetup.locationPoint,
                location,
                radius * 1000
        );
    }

    public List<Meetup> findAllByDistanceAndPredicates(
            Point location,
            double radius,
            @Nullable MainCategory category,
            @Nullable String keyword,
            UUID viewerMemberId
    ) {
        QMeetup meetup = QMeetup.meetup;
        QParticipant participant = QParticipant.participant;
        QUserBlock userBlock = QUserBlock.userBlock;

        BooleanBuilder builder = new BooleanBuilder();

        builder.and(meetup.locationPoint.isNotNull());
        builder.and(distanceWithin(location, radius));
        builder.and(meetup.status.eq(MeetupStatus.OPEN));
        if (category != null) {
            builder.and(meetup.category.eq(category));
        }
        if (keyword != null && !keyword.isEmpty()) {
            builder.and(
                    meetup.name.containsIgnoreCase(keyword)
                    .or(meetup.description.containsIgnoreCase(keyword))
            );
        }
        // 차단 제외 조건
        BooleanExpression blockedWithOwner = JPAExpressions.selectOne().from(userBlock)
            .where(
                userBlock.blockerId.eq(viewerMemberId).and(userBlock.blockedId.eq(meetup.owner.memberId))
                    .or(userBlock.blockerId.eq(meetup.owner.memberId).and(userBlock.blockedId.eq(viewerMemberId)))
            ).exists();

        BooleanExpression blockedWithAnyParticipant = JPAExpressions.selectOne()
            .from(participant)
            .join(userBlock).on(
                userBlock.blockerId.eq(viewerMemberId).and(userBlock.blockedId.eq(participant.profile.memberId))
                    .or(userBlock.blockerId.eq(participant.profile.memberId).and(userBlock.blockedId.eq(viewerMemberId)))
            )
            .where(participant.meetup.id.eq(meetup.id))
            .exists();

        BooleanExpression blockCondition = blockedWithOwner.or(blockedWithAnyParticipant).not();

        return jpaQueryFactory
                .selectFrom(meetup)
                .leftJoin(meetup.hashTags).fetchJoin()
                .where(builder.and(blockCondition))
                .orderBy(meetup.createdAt.desc())
                .fetch();
    }

    public List<Meetup> findAllByOwnerIdAndStatusIn(UUID profileId, List<MeetupStatus> statuses) {
        QMeetup meetup = QMeetup.meetup;

        return jpaQueryFactory
                .selectFrom(meetup)
                .leftJoin(meetup.hashTags).fetchJoin()
                .where(
                    meetup.owner.id.eq(profileId)
                    .and(meetup.status.in(statuses))
                )
                .orderBy(meetup.createdAt.desc())
                .fetch();
    }

    public Optional<Meetup> findParticipatedMeetupsByProfileId(UUID profileId) {
        QMeetup meetup = QMeetup.meetup;
        QParticipant participant = QParticipant.participant;

        return Optional.ofNullable(
                jpaQueryFactory
                    .select(meetup)
                    .from(meetup)
                    .join(meetup.participants, participant)
                    .where(
                        participant.profile.id.eq(profileId)
                        .and(meetup.status.in(MeetupStatus.OPEN, MeetupStatus.IN_PROGRESS))
                    )
                    .fetchFirst()
        );
    }


    public boolean existsParticipatedMeetupByProfileId(UUID profileId) {
        QMeetup meetup = QMeetup.meetup;

        Integer result = jpaQueryFactory
                .selectOne()
                .from(meetup)
                .where(
                    meetup.owner.id.eq(profileId)
                    .and(meetup.status.in(MeetupStatus.OPEN, MeetupStatus.IN_PROGRESS))
                )
                .limit(1)
                .fetchFirst();
        return result != null;
    }

    public Optional<Meetup> findByIdWithDetails(UUID meetupId) {
        QMeetup meetup = QMeetup.meetup;
        QProfile profile = QProfile.profile;
        QMeetupHashTag hashTag = QMeetupHashTag.meetupHashTag;
        QSigungu sigungu = QSigungu.sigungu;

        return Optional.ofNullable(
                jpaQueryFactory
                    .select(meetup)
                    .from(meetup)
                    .leftJoin(meetup.owner, profile).fetchJoin()
                    .leftJoin(meetup.hashTags, hashTag).fetchJoin()
                    .leftJoin(meetup.sigungu, sigungu).fetchJoin()
                    .where(meetup.id.eq(meetupId))
                    .fetchOne()
        );
    }

    public Page<Meetup> findEndedMeetupsByProfileId(UUID profileId, Pageable pageable) {
        return findEndedMeetupsByProfileIdAndEvaluated(profileId, null, pageable);
    }

    public Page<Meetup> findEndedMeetupsByProfileIdAndEvaluated(
        UUID profileId,
        Boolean evaluated,
        Pageable pageable
    ) {
        QMeetup m = QMeetup.meetup;
        QParticipant p = QParticipant.participant;
        QEvaluation e = QEvaluation.evaluation;

        // 기본 조건: 종료 + 종료 3일 이내 + 참가자에 자신 포함
        BooleanExpression base = m.status.eq(MeetupStatus.ENDED)
            .and(m.endAt.goe(LocalDateTime.now().minusDays(EVALUATION_VALID_DAYS)))
            .and(p.profile.id.eq(profileId));

        // 평가 여부 조건
        BooleanExpression evalCond = null;
        if (evaluated != null) {
            BooleanExpression existsMine = JPAExpressions
                .selectOne()
                .from(e)
                .where(e.meetupId.eq(m.id)
                    .and(e.evaluatorProfileId.eq(profileId)))
                .exists();
            evalCond = (evaluated) ? existsMine : existsMine.not();
        }

        // 최종 조건
        BooleanExpression where = (evalCond == null) ? base : base.and(evalCond);

        List<Meetup> content = jpaQueryFactory
            .selectFrom(m)
            .join(m.participants, p).fetchJoin()
            .where(where)
            .distinct()
            .orderBy(m.endAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(m.countDistinct())
            .from(m)
            .join(m.participants, p)
            .where(where)
            .fetchOne();

        return new PageImpl<>(content, pageable, (total == null) ? 0 : total);
    }

    public boolean existsBlockedInMeetup(UUID meetupId, UUID viewerMemberId) {
        QMeetup m = QMeetup.meetup;
        QParticipant p = QParticipant.participant;
        QUserBlock ub = QUserBlock.userBlock;

        // owner 차단
        BooleanExpression ownerBlocked = JPAExpressions.selectOne().from(m).join(ub)
            .on(
                ub.blockerId.eq(viewerMemberId).and(ub.blockedId.eq(m.owner.memberId))
                    .or(ub.blockerId.eq(m.owner.memberId).and(ub.blockedId.eq(viewerMemberId)))
            )
            .where(m.id.eq(meetupId))
            .exists();

        // 참가자 중 1명이라도 차단
        BooleanExpression anyParticipantBlocked = JPAExpressions.selectOne().from(p).join(ub)
            .on(
                ub.blockerId.eq(viewerMemberId).and(ub.blockedId.eq(p.profile.memberId))
                    .or(ub.blockerId.eq(p.profile.memberId).and(ub.blockedId.eq(viewerMemberId)))
            )
            .where(p.meetup.id.eq(meetupId))
            .exists();

        BooleanExpression condition = m.id.eq(meetupId).and(ownerBlocked.or(anyParticipantBlocked));

        Integer one = jpaQueryFactory.selectOne()
            .from(m)
            .where(condition)
            .fetchFirst();
        return one != null;
    }
}
