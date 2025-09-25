package com.pnu.momeet.domain.badge.repository;

import com.pnu.momeet.domain.badge.entity.Badge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    @Query("select b.id from Badge b where LOWER(b.code) = LOWER(:code)")
    Optional<UUID> findIdByCode(@Param("code") String code);
}
