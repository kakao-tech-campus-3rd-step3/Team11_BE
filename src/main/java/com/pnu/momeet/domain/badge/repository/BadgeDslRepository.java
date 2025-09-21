package com.pnu.momeet.domain.badge.repository;

import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.entity.QBadge;
import com.pnu.momeet.domain.badge.entity.QProfileBadge;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BadgeDslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<BadgeResponse> findBadgesByProfileId(UUID profileId, Pageable pageable) {
        QProfileBadge pb = QProfileBadge.profileBadge;
        QBadge b = QBadge.badge;

        var base = jpaQueryFactory
            .select(Projections.constructor(
                BadgeResponse.class,
                b.id,
                b.name,
                b.description,
                b.iconUrl,
                pb.createdAt,
                pb.representative
            ))
            .from(pb)
            .join(b).on(b.id.eq(pb.badgeId))
            .where(pb.profileId.eq(profileId));

        OrderSpecifier<?>[] orders = toOrderSpecifiers(pageable.getSort(), pb, b);

        List<BadgeResponse> content = base
            .orderBy(orders.length == 0 ? defaultOrders(pb) : orders)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(pb.count())
            .from(pb)
            .where(pb.profileId.eq(profileId))
            .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private OrderSpecifier<?>[] defaultOrders(QProfileBadge pb) {
        return new OrderSpecifier<?>[]{
            new OrderSpecifier<>(Order.DESC, pb.representative),
            new OrderSpecifier<>(Order.DESC, pb.createdAt)
        };
    }

    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort, QProfileBadge pb, QBadge b) {
        if (sort == null || sort.isEmpty()) return new OrderSpecifier<?>[0];

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order o : sort) {
            boolean asc = o.getDirection().isAscending();
            String prop = o.getProperty();
            switch (prop) {
                case "representative" -> orders.add(asc ? pb.representative.asc() : pb.representative.desc());
                case "createdAt"     -> orders.add(asc ? pb.createdAt.asc()     : pb.createdAt.desc());
                case "name"          -> orders.add(asc ? b.name.asc()           : b.name.desc());
                default              -> { /* 무시(허용 외 필드) */ }
            }
        }
        return orders.toArray(OrderSpecifier[]::new);
    }
}
