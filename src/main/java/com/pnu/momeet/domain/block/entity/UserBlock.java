package com.pnu.momeet.domain.block.entity;

import com.pnu.momeet.domain.common.entity.BaseCreatedEntity;
import com.pnu.momeet.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_block")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserBlock extends BaseCreatedEntity {

    @Column(name = "blocker_id", nullable = false)
    private UUID blockerId;

    @Column(name = "blocked_id", nullable = false)
    private UUID blockedId;

    public static UserBlock create(UUID blockerId, UUID blockedId) {
        return new UserBlock(blockerId, blockedId);
    }
}
