package com.pnu.momeet.domain.block.service;

import com.pnu.momeet.domain.block.dto.response.BlockResponse;
import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.block.mapper.BlockEntityMapper;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockDomainService {

    private final BlockEntityService entityService;
    private final MemberEntityService memberService;

    @Transactional
    public BlockResponse createUserBlock(UUID me, UUID targetId) {
        // 1. 대상 회원 존재 검증
        if (!memberService.existsById(targetId)) {
            log.info("차단 대상 사용자 조회 실패. blockerId={}, blockedId={}", me, targetId);
            throw new NoSuchElementException("대상 사용자를 찾을 수 없습니다.");
        }
        // 2. 자기 자신 차단 금지
        if (me.equals(targetId)) {
            log.info("자기 자신 차단 시도. blockerId={}, blockedId={}", me, targetId);
            throw new IllegalArgumentException("자기 자신은 차단할 수 없습니다.");
        }
        // 3. 이미 차단한 사용자면 금지
        if (entityService.exists(me, targetId)) {
            log.info("이미 차단한 사용자 차단 시도. blockerId={}, blockedId={}", me, targetId);
            throw new IllegalStateException("이미 차단한 사용자입니다.");
        }

        try {
            UserBlock block = entityService.save(me, targetId);
            log.info("차단 완료. blockerId={}, blockedId={}", me, targetId);
            return BlockEntityMapper.toBlockResponse(block);
        } catch (DataIntegrityViolationException e) {
            log.info("경쟁 상태 중복 차단. blockerId={}, blockedId={}", me, targetId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 차단한 사용자입니다.");
        }
    }
}
