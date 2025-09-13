package com.pnu.momeet.domain.meetup.repository;

import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.entity.QMeetup;
import com.pnu.momeet.domain.profile.entity.QProfile;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MeetupGeoRepository {
    private final JPAQueryFactory jpaQueryFactory;
    public MeetupGeoRepository(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    private BooleanExpression distanceWithin(Point location, double radius) {
        return Expressions.booleanTemplate(
                "ST_DWithin({0}, {1}, {2})",
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

        return jpaQueryFactory.selectFrom(meetup)
                .leftJoin(meetup.owner, profile).fetchJoin()
                .where(
                        meetup.locationPoint.isNotNull(),
                        distanceWithin(location, radius),
                        meetup.status.eq(MeetupStatus.OPEN),
                        category.map(c -> meetup.category.eq(c)).orElse(null),
                        subCategory.map(sc -> meetup.subCategory.eq(sc)).orElse(null),
                        keyword.map(kw -> meetup.name.containsIgnoreCase(kw)
                                .or(meetup.description.containsIgnoreCase(kw)))
                                .orElse(null)
                .fetch();
    }
}
