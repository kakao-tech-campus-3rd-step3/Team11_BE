package com.pnu.momeet.domain.block.repository;

import com.pnu.momeet.domain.block.entity.UserBlock;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlockerProfileIdAndBlockedProfileId(UUID blockerProfileId, UUID blockedProfileId);
    long deleteByBlockerProfileIdAndBlockedProfileId(UUID blockerProfileId, UUID blockedProfileId);
}
