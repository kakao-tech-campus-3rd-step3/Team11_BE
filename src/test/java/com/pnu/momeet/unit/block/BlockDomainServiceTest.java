package com.pnu.momeet.unit.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.block.dto.request.BlockPageRequest;
import com.pnu.momeet.domain.block.dto.response.BlockResponse;
import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.block.service.BlockDomainService;
import com.pnu.momeet.domain.block.service.BlockEntityService;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import com.pnu.momeet.domain.profile.dto.response.BlockedProfileResponse;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BlockDomainServiceTest {

    @Mock
    private BlockEntityService entityService;
    @Mock private MemberEntityService memberService;
    @Mock private ProfileEntityService profileService;

    @InjectMocks
    private BlockDomainService domainService;

    @Test
    @DisplayName("createUserBlock: 정상 생성 → BlockResponse 반환")
    void createUserBlock_success() {
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        given(memberService.existsById(target)).willReturn(true);
        given(entityService.exists(me, target)).willReturn(false);

        // save가 반환할 엔티티에 createdAt까지 세팅
        var block = UserBlock.create(me, target);
        // BaseCreatedEntity의 createdAt을 흉내내 세팅
        org.springframework.test.util.ReflectionTestUtils
            .setField(block, "createdAt", LocalDateTime.now());
        given(entityService.save(me, target)).willReturn(block);

        BlockResponse resp = domainService.createUserBlock(me, target);

        assertThat(resp.blockerId()).isEqualTo(me);
        assertThat(resp.blockedId()).isEqualTo(target);
        assertThat(resp.createdAt()).isNotNull();
        verify(entityService).save(me, target);
    }

    @Test
    @DisplayName("createUserBlock: 대상 회원 없음 → NoSuchElementException")
    void createUserBlock_targetNotFound() {
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        given(memberService.existsById(target)).willReturn(false);

        assertThatThrownBy(() -> domainService.createUserBlock(me, target))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("대상 사용자를 찾을 수 없습니다.");

        verify(entityService, never()).save(any(), any());
    }

    @Test
    @DisplayName("createUserBlock: 자기 자신 차단 → IllegalArgumentException")
    void createUserBlock_selfBlock() {
        UUID me = UUID.randomUUID();

        assertThatThrownBy(() -> domainService.createUserBlock(me, me))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("자기 자신은 차단할 수 없습니다.");

        verify(entityService, never()).save(any(), any());
    }

    @Test
    @DisplayName("createUserBlock: 이미 차단됨 → IllegalStateException")
    void createUserBlock_duplicate() {
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        given(memberService.existsById(target)).willReturn(true);
        given(entityService.exists(me, target)).willReturn(true);

        assertThatThrownBy(() -> domainService.createUserBlock(me, target))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 차단한 사용자입니다.");

        verify(entityService, never()).save(any(), any());
    }

    @Test
    @DisplayName("createUserBlock: 동시성으로 UNIQUE 위반 → ResponseStatusException(409)")
    void createUserBlock_raceConflict() {
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        given(memberService.existsById(target)).willReturn(true);
        given(entityService.exists(me, target)).willReturn(false);
        given(entityService.save(me, target))
            .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> domainService.createUserBlock(me, target))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .extracting("statusCode").asString().contains("409");
    }

    @Test
    @DisplayName("deleteBlock: 항상 위임(idempotent)")
    void deleteBlock_alwaysDelegates() {
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        given(entityService.delete(me, target)).willReturn(1L);

        domainService.deleteBlock(me, target);

        verify(entityService).delete(me, target);
    }

    @Test
    @DisplayName("getMyBlockedProfiles: ProfileEntityService에 Pageable 전달 & 결과 반환")
    void getMyBlockedProfiles_delegatesToProfileService() {
        UUID me = UUID.randomUUID();

        // 요청 DTO
        BlockPageRequest req = new BlockPageRequest();
        req.setPage(0);
        req.setSize(20);

        // Stub Page
        Pageable pageable = PageRequest.of(0, 20);
        var row = new BlockedProfileResponse(
            UUID.randomUUID(), "닉", null, null, LocalDateTime.now()
        );
        Page<BlockedProfileResponse> stub = new PageImpl<>(List.of(row), pageable, 1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        given(profileService.getBlockedProfiles(eq(me), any(Pageable.class))).willReturn(stub);

        Page<BlockedProfileResponse> out = domainService.getMyBlockedProfiles(me, req);

        assertThat(out.getTotalElements()).isEqualTo(1);
        verify(profileService).getBlockedProfiles(eq(me), captor.capture());
        Pageable used = captor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20); // 현재 mapper는 size 상한 없이 그대로 사용
    }
}
