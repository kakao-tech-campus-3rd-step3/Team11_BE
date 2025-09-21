package com.pnu.momeet.domain.badge.entity;

import com.pnu.momeet.domain.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profile_badge",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"profile_id", "badge_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProfileBadge extends BaseCreatedEntity {

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "badge_id", nullable = false)
    private UUID badgeId;

    @Column(name = "is_representative", nullable = false)
    private boolean representative = false;
}
