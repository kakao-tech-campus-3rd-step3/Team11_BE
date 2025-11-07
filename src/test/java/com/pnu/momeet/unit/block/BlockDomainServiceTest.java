package com.pnu.momeet.unit.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.pnu.momeet.domain.block.dto.request.BlockPageRequest;
import com.pnu.momeet.domain.block.dto.response.BlockResponse;
import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.block.service.BlockDomainService;
import com.pnu.momeet.domain.block.service.BlockEntityService;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import com.pnu.momeet.domain.profile.dto.response.BlockedProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.lang.reflect.Constructor;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BlockDomainServiceTest {

    @Mock private BlockEntityService entityService;
    @Mock private ProfileEntityService profileService;
    @InjectMocks private BlockDomainService domainService;

    // 프로필 더미 생성 헬퍼
    private static Profile profileWithId(UUID id) {
        try {
            Constructor<Profile> ctor = Profile.class.getDeclaredConstructor();
            ctor.setAccessible(true);                  // protected 생성자 우회
            Profile p = ctor.newInstance();
            ReflectionTestUtils.setField(p, "id", id); // BaseEntity.id 직접 주입
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test @DisplayName("createUserBlock: 정상 생성 → BlockResponse 반환")
    void createUserBlock_success() {
        UUID meMember = UUID.randomUUID();
        UUID myProfile = UUID.randomUUID();
        UUID targetProfile = UUID.randomUUID();

        given(profileService.getByMemberId(meMember)).willReturn(profileWithId(myProfile));
        given(profileService.existsById(targetProfile)).willReturn(true);
        given(entityService.exists(myProfile, targetProfile)).willReturn(false);

        var block = com.pnu.momeet.domain.block.entity.UserBlock.create(myProfile, targetProfile);
        ReflectionTestUtils.setField(block, "createdAt", LocalDateTime.now());
        given(entityService.save(myProfile, targetProfile)).willReturn(block);

        BlockResponse resp = domainService.createUserBlock(meMember, targetProfile);

        assertThat(resp.blockerProfileId()).isEqualTo(myProfile);
        assertThat(resp.blockedProfileId()).isEqualTo(targetProfile);
        assertThat(resp.createdAt()).isNotNull();
        verify(entityService).save(myProfile, targetProfile);
    }

    @Test @DisplayName("createUserBlock: 대상 프로필 없음 → NoSuchElementException")
    void createUserBlock_targetNotFound() {
        UUID meMember = UUID.randomUUID();
        UUID myProfile = UUID.randomUUID();
        UUID targetProfile = UUID.randomUUID();

        given(profileService.getByMemberId(meMember)).willReturn(profileWithId(myProfile));
        given(profileService.existsById(targetProfile)).willReturn(false);

        assertThatThrownBy(() -> domainService.createUserBlock(meMember, targetProfile))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("대상 사용자 프로필을 찾을 수 없습니다.");

        verify(entityService, never()).save(any(), any());
    }

    @Test @DisplayName("createUserBlock: 자기 자신 차단 → IllegalArgumentException")
    void createUserBlock_selfBlock() {
        UUID meMember = UUID.randomUUID();
        UUID myProfile = UUID.randomUUID();

        given(profileService.getByMemberId(meMember)).willReturn(profileWithId(myProfile));

        assertThatThrownBy(() -> domainService.createUserBlock(meMember, myProfile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("자기 자신은 차단할 수 없습니다.");

        verify(entityService, never()).save(any(), any());
    }

    @Test @DisplayName("createUserBlock: 이미 차단됨 → IllegalStateException")
    void createUserBlock_duplicate() {
        UUID meMember = UUID.randomUUID();
        UUID myProfile = UUID.randomUUID();
        UUID targetProfile = UUID.randomUUID();

        given(profileService.getByMemberId(meMember)).willReturn(profileWithId(myProfile));
        given(profileService.existsById(targetProfile)).willReturn(true);
        given(entityService.exists(myProfile, targetProfile)).willReturn(true);

        assertThatThrownBy(() -> domainService.createUserBlock(meMember, targetProfile))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 차단한 사용자 프로필입니다.");

        verify(entityService, never()).save(any(), any());
    }

    @Test @DisplayName("createUserBlock: 동시성 UNIQUE 위반 → DataIntegrityViolationException 전파")
    void createUserBlock_raceConflict() {
        UUID meMember = UUID.randomUUID();
        UUID myProfile = UUID.randomUUID();
        UUID targetProfile = UUID.randomUUID();

        given(profileService.getByMemberId(meMember)).willReturn(profileWithId(myProfile));
        given(profileService.existsById(targetProfile)).willReturn(true);
        given(entityService.exists(myProfile, targetProfile)).willReturn(false);
        given(entityService.save(myProfile, targetProfile))
            .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> domainService.createUserBlock(meMember, targetProfile))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test @DisplayName("deleteBlock: 항상 위임(idempotent)")
    void deleteBlock_alwaysDelegates() {
        UUID meMember = UUID.randomUUID();
        UUID myProfile = UUID.randomUUID();
        UUID targetProfile = UUID.randomUUID();

        given(profileService.getByMemberId(meMember)).willReturn(profileWithId(myProfile));
        given(entityService.delete(myProfile, targetProfile)).willReturn(1L);

        domainService.deleteBlock(meMember, targetProfile);

        verify(entityService).delete(myProfile, targetProfile);
    }

    @Test @DisplayName("getMyBlockedProfiles: ProfileEntityService에 (내 profileId, Pageable) 전달")
    void getMyBlockedProfiles_delegatesToProfileService() {
        UUID meMember = UUID.randomUUID();
        UUID myProfile = UUID.randomUUID();

        var req = new BlockPageRequest();
        req.setPage(0); req.setSize(20);

        var pageable = PageRequest.of(0, 20);
        var row = new BlockedProfileResponse(
            UUID.randomUUID(), "닉", null, null, LocalDateTime.now()
        );
        var stub = new PageImpl<>(List.of(row), pageable, 1);

        given(profileService.getByMemberId(meMember)).willReturn(profileWithId(myProfile));
        given(profileService.getBlockedProfiles(eq(myProfile), any(Pageable.class)))
            .willReturn(stub);

        var out = domainService.getMyBlockedProfiles(meMember, req);

        assertThat(out.getTotalElements()).isEqualTo(1);
        verify(profileService).getBlockedProfiles(eq(myProfile), any(Pageable.class));
    }
}