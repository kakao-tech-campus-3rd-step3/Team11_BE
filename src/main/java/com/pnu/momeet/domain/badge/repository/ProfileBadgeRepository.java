package com.pnu.momeet.domain.badge.repository;

import com.pnu.momeet.domain.badge.entity.ProfileBadge;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileBadgeRepository extends JpaRepository<ProfileBadge, UUID> {
}
