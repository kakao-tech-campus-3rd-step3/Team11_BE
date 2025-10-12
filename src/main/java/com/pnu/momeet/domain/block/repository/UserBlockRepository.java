package com.pnu.momeet.domain.block.repository;

import com.pnu.momeet.domain.block.entity.UserBlock;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}
