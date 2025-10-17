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
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional
    public UserBlock save(UUID blockerId, UUID blockedId) {
        log.debug("특정 사용자 차단 시도. blockerId={}, blockedId={}", blockerId, blockedId);
        UserBlock block = UserBlock.create(blockerId, blockedId);
        UserBlock saved = blockRepository.save(block);
        log.debug("특정 사용자 차단 성공. blockerId={}, blockedId={}", blockerId, blockedId);
        return saved;
    }

    @Transactional
    public long delete(UUID blockerId, UUID blockedId) {
        return blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }
}
