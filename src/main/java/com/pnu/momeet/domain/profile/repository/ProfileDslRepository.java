package com.pnu.momeet.domain.profile.repository;

import com.pnu.momeet.domain.block.entity.QUserBlock;
import com.pnu.momeet.domain.profile.dto.response.BlockedProfileResponse;
import com.pnu.momeet.domain.profile.entity.QProfile;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ProfileDslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Transactional(readOnly = true)
    public Page<BlockedProfileResponse> findBlockedProfiles(UUID blockerId, Pageable pageable) {
        QUserBlock b = QUserBlock.userBlock;
        QProfile p = QProfile.profile;

        // content
        List<BlockedProfileResponse> content = jpaQueryFactory
            .select(Projections.constructor(
                BlockedProfileResponse.class,
                p.id,
                p.nickname,
                p.imageUrl,
                p.temperature,
                b.createdAt     // blockedAt
            ))
            .from(b)
            .join(p).on(p.memberId.eq(b.blockedId))
            .where(b.blockerId.eq(blockerId))
            .orderBy(b.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        // count
        Long total = jpaQueryFactory
            .select(b.count())
            .from(b)
            .where(b.blockerId.eq(blockerId))
            .fetchOne();

        long totalElements = total == null ? 0L : total;
        return new PageImpl<>(content, pageable, totalElements);
    }
}
