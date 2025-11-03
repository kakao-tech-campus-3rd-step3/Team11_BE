package com.pnu.momeet.domain.block.service;

import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.block.repository.BlockRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockEntityService {

    private final BlockRepository blockRepository;

    @Transactional(readOnly = true)
    public boolean exists(UUID blockerId, UUID blockedId) {
        return blockRepository.existsByBlockerProfileIdAndBlockedProfileId(blockerId, blockedId);
    }

    @Transactional
    public UserBlock save(UUID blockerProfileId, UUID blockedProfileId) {
        log.debug("특정 사용자 프로필 차단 시도. blockerProfileId={}, blockedProfileId={}", blockerProfileId, blockedProfileId);
        UserBlock block = UserBlock.create(blockerProfileId, blockedProfileId);
        UserBlock saved = blockRepository.save(block);
        log.debug("특정 사용자 프로필 차단 성공. blockerProfileId={}, blockedProfileId={}", blockerProfileId, blockedProfileId);
        return saved;
    }

    @Transactional
    public long delete(UUID blockerProfileId, UUID blockedProfileId) {
        return blockRepository.deleteByBlockerProfileIdAndBlockedProfileId(blockerProfileId, blockedProfileId);
    }
}
