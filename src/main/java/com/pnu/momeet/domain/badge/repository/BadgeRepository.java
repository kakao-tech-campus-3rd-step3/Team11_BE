package com.pnu.momeet.domain.badge.repository;

import com.pnu.momeet.domain.badge.entity.Badge;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {
}
