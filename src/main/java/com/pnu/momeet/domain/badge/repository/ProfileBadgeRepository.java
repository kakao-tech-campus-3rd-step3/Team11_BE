package com.pnu.momeet.domain.badge.repository;

import com.pnu.momeet.domain.badge.entity.ProfileBadge;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileBadgeRepository extends JpaRepository<ProfileBadge, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO profile_badge(profile_id, badge_id, is_representative, created_at)
        VALUES (:profileId, :badgeId, false, now())
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    void insertIgnore(@Param("profileId") UUID profileId, @Param("badgeId") UUID badgeId);
}
