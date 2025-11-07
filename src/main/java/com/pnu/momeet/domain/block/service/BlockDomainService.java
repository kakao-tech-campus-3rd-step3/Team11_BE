package com.pnu.momeet.domain.block.service;

import com.pnu.momeet.domain.block.dto.request.BlockPageRequest;
import com.pnu.momeet.domain.block.dto.response.BlockResponse;
import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.block.mapper.BlockEntityMapper;
import com.pnu.momeet.domain.block.service.mapper.BlockDtoMapper;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import com.pnu.momeet.domain.profile.dto.response.BlockedProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockDomainService {

    private final BlockEntityService entityService;
    private final ProfileEntityService profileService;

    @Transactional(readOnly = true)
    public Page<BlockedProfileResponse> getMyBlockedProfiles(
        UUID me,
        BlockPageRequest pageRequest
    ) {
        Profile myProfile = profileService.getByMemberId(me);
        log.debug("차단 목록 조회 시도. blockerProfileId={}", myProfile.getId());
        PageRequest page = BlockDtoMapper.toPageRequest(pageRequest);
        Page<BlockedProfileResponse> blockedProfiles = profileService.getBlockedProfiles(myProfile.getId(), page);
        log.debug("차단 목록 조회 성공. blockerProfileId={}", myProfile.getId());
        return blockedProfiles;
    }

    @Transactional
    public BlockResponse createUserBlock(UUID me, UUID targetProfileId) {
        Profile myProfile = profileService.getByMemberId(me);
        // 1. 자기 자신 차단 금지
        if (myProfile.getId().equals(targetProfileId)) {
            log.info("자기 자신 차단 시도. blockerProfileId={}, blockedProfileId={}", myProfile.getId(), targetProfileId);
            throw new IllegalArgumentException("자기 자신은 차단할 수 없습니다.");
        }
        // 2. 대상 회원 프로필 존재 검증
        if (!profileService.existsById(targetProfileId)) {
            log.info("차단 대상 사용자 프로필 조회 실패. blockerProfileId={}, blockedProfileId={}", myProfile.getId(), targetProfileId);
            throw new NoSuchElementException("대상 사용자 프로필을 찾을 수 없습니다.");
        }
        // 3. 이미 차단한 사용자면 금지
        if (entityService.exists(myProfile.getId(), targetProfileId)) {
            log.info("이미 차단한 사용자 프로필 차단 시도. blockerProfileId={}, blockedProfileId={}", myProfile.getId(), targetProfileId);
            throw new IllegalStateException("이미 차단한 사용자 프로필입니다.");
        }

        UserBlock block = entityService.save(myProfile.getId(), targetProfileId);
        log.info("차단 완료. blockerProfileId={}, blockedProfileId={}", myProfile.getId(), targetProfileId);
        return BlockEntityMapper.toBlockResponse(block);
    }

    @Transactional
    public void deleteBlock(UUID me, UUID targetProfileId) {
        Profile myProfile = profileService.getByMemberId(me);
        entityService.delete(myProfile.getId(), targetProfileId);
        log.info("차단 해제 완료. blockerProfileId={}, blockedProfileId={}", myProfile.getId(), targetProfileId);
    }
}
