package com.pnu.momeet.domain.meetup.repository;

import com.pnu.momeet.domain.meetup.entity.QMeetupHashTag;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.participant.entity.QParticipant;
import com.pnu.momeet.domain.sigungu.entity.QSigungu;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.entity.QMeetup;
import com.pnu.momeet.domain.profile.entity.QProfile;
import java.time.LocalDateTime;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MeetupDslRepository {
    private static final int EVALUATION_VALID_DAYS = 3;
    private final JPAQueryFactory jpaQueryFactory;
    public MeetupDslRepository(JPAQueryFactory jpaQueryFactory) {
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
            Optional<MainCategory> category,
            Optional<SubCategory> subCategory,
            Optional<String> keyword
    ) {
        QMeetup meetup = QMeetup.meetup;
        QProfile profile = QProfile.profile;
        QMeetupHashTag hashTag = QMeetupHashTag.meetupHashTag;

        BooleanBuilder builder = new BooleanBuilder();

        builder.and(meetup.locationPoint.isNotNull());
        builder.and(distanceWithin(location, radius));
        builder.and(meetup.status.eq(MeetupStatus.OPEN));
        category.ifPresent(mainCategory -> builder.and(meetup.category.eq(mainCategory)));
        subCategory.ifPresent(subCat -> builder.and(meetup.subCategory.eq(subCat)));
        keyword.ifPresent(kw -> builder.and(
                meetup.name.containsIgnoreCase(kw)
                .or(meetup.description.containsIgnoreCase(kw))
        ));

        return jpaQueryFactory
                .selectFrom(meetup)
                .leftJoin(meetup.owner, profile).fetchJoin()
                .leftJoin(meetup.hashTags, hashTag).fetchJoin()
                .where(builder)
                .orderBy(meetup.createdAt.desc())
                .fetch();
    }

    public Optional<Meetup> findOwnedMeetupByMemberId(UUID memberId) {
        QMeetup meetup = QMeetup.meetup;

        return Optional.ofNullable(
                jpaQueryFactory
                    .select(meetup)
                    .from(meetup)
                    .where(
                        meetup.owner.memberId.eq(memberId)
                        .and(
                            meetup.status.eq(MeetupStatus.OPEN)
                            .or(meetup.status.eq(MeetupStatus.IN_PROGRESS))
                        )
                    )
                    .fetchOne()
        );
    }

    public boolean existsByOwnerIdAndStatusIn(UUID memberId, List<MeetupStatus> statuses) {
        QMeetup meetup = QMeetup.meetup;

        Integer result = jpaQueryFactory
                .selectOne()
                .from(meetup)
                .where(
                    meetup.owner.memberId.eq(memberId)
                    .and(meetup.status.in(statuses))
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


    public Optional<Meetup> findOwnedMeetupByMemberIdWithDetails(UUID memberId) {
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
                    .where(
                        meetup.owner.memberId.eq(memberId)
                        .and(
                            meetup.status.eq(MeetupStatus.OPEN)
                            .or(meetup.status.eq(MeetupStatus.IN_PROGRESS))
                        )
                    )
                    .fetchOne()
        );
    }

    public Page<Meetup> findEndedMeetupsByProfileId(UUID profileId, Pageable pageable) {
        QMeetup m = QMeetup.meetup;
        QParticipant p = QParticipant.participant;

        List<Meetup> content = jpaQueryFactory
            .selectFrom(m)
            .join(m.participants, p).fetchJoin()
            .where(
                m.status.eq(MeetupStatus.ENDED),
                m.endAt.goe(LocalDateTime.now().minusDays(EVALUATION_VALID_DAYS)),
                p.profile.id.eq(profileId)
            )
            .distinct()
            .orderBy(m.endAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(m.countDistinct())
            .from(m)
            .join(m.participants, p)
            .where(
                m.status.eq(MeetupStatus.ENDED),
                p.profile.id.eq(profileId)
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
