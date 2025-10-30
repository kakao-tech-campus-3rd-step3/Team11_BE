package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgeRepresentativeRequest;
import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.entity.ProfileBadge;
import com.pnu.momeet.domain.badge.service.BadgeDomainService;
import com.pnu.momeet.domain.badge.service.BadgeEntityService;
import com.pnu.momeet.domain.badge.service.ProfileBadgeDomainService;
import com.pnu.momeet.domain.badge.service.ProfileBadgeEntityService;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ProfileBadgeDomainServiceTest {

    @Mock private ProfileBadgeEntityService entityService;
    @Mock private BadgeDomainService badgeService;
    @Mock private ProfileDomainService profileService;

    @InjectMocks
    private ProfileBadgeDomainService profileBadgeService;

    @Test
    @DisplayName("내 배지 조회 성공 - Profile 검증 후 EntityService 위임")
    void getMyBadges_success() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        ProfileResponse profile = new ProfileResponse(
            profileId, "닉", 20, Gender.MALE, null, "소개",
            26410L, "부산 북구",
            BigDecimal.valueOf(36.5), 0, 0,
            LocalDateTime.now().minusDays(2), LocalDateTime.now()
        );
        given(profileService.getProfileByMemberId(memberId)).willReturn(profile);

        ProfileBadgePageRequest req = new ProfileBadgePageRequest();
        req.setPage(0); req.setSize(5); req.setSort("representative,DESC,createdAt,DESC");

        var rows = java.util.List.of(
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "첫 배지", "첫 배지",
                "https://icon/1.png", "FIRST",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                true
            ),
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "둘째 배지", "둘째 배지",
                "https://icon/2.png", "SECOND",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(2),
                false
            )
        );
        Page<ProfileBadgeResponse> fake = new PageImpl<>(rows, PageRequest.of(0,5), 2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(entityService.findBadgesByProfileId(eq(profileId), any(Pageable.class))).willReturn(fake);

        // when
        Page<ProfileBadgeResponse> result = profileBadgeService.getMyBadges(memberId, req);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(profileService).getProfileByMemberId(memberId);
        verify(entityService).findBadgesByProfileId(eq(profileId), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort().isSorted()).isTrue();
    }

    @Test
    @DisplayName("내 배지 조회 실패 - 프로필 없음")
    void getMyBadges_fail_profileNotFound() {
        UUID memberId = UUID.randomUUID();
        ProfileBadgePageRequest req = new ProfileBadgePageRequest();
        req.setPage(0); req.setSize(10);

        given(profileService.getProfileByMemberId(memberId))
            .willThrow(new NoSuchElementException("프로필이 존재하지 않습니다."));

        assertThatThrownBy(() -> profileBadgeService.getMyBadges(memberId, req))
            .isInstanceOf(NoSuchElementException.class);

        verify(profileService).getProfileByMemberId(memberId);
        verify(entityService, never()).findBadgesByProfileId(any(), any());
    }

    @Test
    @DisplayName("특정 사용자 배지 조회 성공 - 프로필 존재 검증 후 EntityService 위임")
    void getUserBadges_success() {
        UUID profileId = UUID.randomUUID();

        ProfileResponse dummy = new ProfileResponse(
            profileId, "닉", 20, Gender.FEMALE, null, "소개",
            26410L, "부산 북구",
            BigDecimal.valueOf(36.5), 0, 0,
            LocalDateTime.now().minusDays(3), LocalDateTime.now()
        );
        given(profileService.getProfileById(profileId)).willReturn(dummy);

        ProfileBadgePageRequest req = new ProfileBadgePageRequest();
        req.setPage(1); req.setSize(3); req.setSort("representative,DESC,createdAt,DESC");

        var rows = java.util.List.of(
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "배지A", "설명A",
                "https://icon/a.png", "A",
                LocalDateTime.now().minusHours(5),
                LocalDateTime.now().minusHours(5),
                true
            ),
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "배지B", "설명B",
                "https://icon/b.png", "B",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                false
            ),
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "배지C", "설명C",
                "https://icon/c.png", "C",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(2),
                false
            )
        );
        Page<ProfileBadgeResponse> fake = new PageImpl<>(rows, PageRequest.of(1,3), 7);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(entityService.findBadgesByProfileId(eq(profileId), any(Pageable.class))).willReturn(fake);

        Page<ProfileBadgeResponse> result = profileBadgeService.getUserBadges(profileId, req);

        assertThat(result.getTotalElements()).isEqualTo(7);
        verify(profileService).getProfileById(profileId);
        verify(entityService).findBadgesByProfileId(eq(profileId), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(3);
        assertThat(used.getSort().isSorted()).isTrue();
    }

    @Test
    @DisplayName("특정 사용자 배지 조회 실패 - 프로필 없음")
    void getUserBadges_fail_profileNotFound() {
        UUID profileId = UUID.randomUUID();
        ProfileBadgePageRequest req = new ProfileBadgePageRequest();
        req.setPage(0); req.setSize(10);

        given(profileService.getProfileById(profileId))
            .willThrow(new NoSuchElementException("프로필이 존재하지 않습니다."));

        assertThatThrownBy(() -> profileBadgeService.getUserBadges(profileId, req))
            .isInstanceOf(NoSuchElementException.class);

        verify(profileService).getProfileById(profileId);
        verify(entityService, never()).findBadgesByProfileId(any(), any());
    }

    @Test
    @DisplayName("내 대표 배지 조회 - 프로필 식별 후 EntityService 위임 (있음)")
    void getMyRepresentative_present() {
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(profileService.getProfileByMemberId(memberId))
            .thenReturn(new ProfileResponse(
                profileId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                null)
            );

        var dto = new ProfileBadgeResponse(
            UUID.randomUUID(), "REP", "대표", "https://rep", "REP_CODE",
            LocalDateTime.now(), LocalDateTime.now(), true
        );
        when(entityService.getRepresentativeByProfileId(profileId))
            .thenReturn(Optional.of(dto));

        var res = profileBadgeService.getMyRepresentativeBadge(memberId);

        assertThat(res).isPresent();
        assertThat(res.get().representative()).isTrue();
        verify(profileService).getProfileByMemberId(memberId);
        verify(entityService).getRepresentativeByProfileId(eq(profileId));
    }

    @Test
    @DisplayName("내 대표 배지 조회 - 프로필 식별 후 EntityService 위임 (없음)")
    void getMyRepresentative_absent() {
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        when(profileService.getProfileByMemberId(memberId))
            .thenReturn(new ProfileResponse(
                profileId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                null)
            );
        when(entityService.getRepresentativeByProfileId(profileId))
            .thenReturn(Optional.empty());

        var res = profileBadgeService.getMyRepresentativeBadge(memberId);

        assertThat(res).isEmpty();
        verify(profileService).getProfileByMemberId(memberId);
        verify(entityService).getRepresentativeByProfileId(eq(profileId));
    }

    @Test
    @DisplayName("특정 프로필 대표 배지 조회 - 프로필 존재 검증 후 EntityService로 위임 (있음)")
    void getUserRepresentative_present() {
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        var stubProfile = new ProfileResponse(
            profileId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            0,
            null,
            null
        );
        when(profileService.getProfileById(profileId)).thenReturn(stubProfile);

        var dto = new ProfileBadgeResponse(
            UUID.randomUUID(), "REP", "대표", "https://rep", "REP_CODE",
            LocalDateTime.now(), LocalDateTime.now(), true
        );
        when(entityService.getRepresentativeByProfileId(profileId))
            .thenReturn(Optional.of(dto));

        var res = profileBadgeService.getUserRepresentativeBadge(profileId);

        assertThat(res).isPresent();
        assertThat(res.get().representative()).isTrue();
        verify(profileService).getProfileById(profileId);
        verify(entityService).getRepresentativeByProfileId(eq(profileId));
    }

    @Test
    @DisplayName("특정 프로필 대표 배지 조회 - 프로필 존재 검증 후 EntityService로 위임 (없음)")
    void getUserRepresentative_absent() {
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        var stubProfile = new ProfileResponse(
            profileId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            0,
            null,
            null
        );
        when(profileService.getProfileById(profileId)).thenReturn(stubProfile);

        when(entityService.getRepresentativeByProfileId(profileId))
            .thenReturn(Optional.empty());

        var res = profileBadgeService.getUserRepresentativeBadge(profileId);

        assertThat(res).isEmpty();
        verify(profileService).getProfileById(profileId);
        verify(entityService).getRepresentativeByProfileId(eq(profileId));
    }

    @Test
    @DisplayName("이미 대표 배지면 멱등 - reset/set 없이 바로 반환")
    void setRepresentative_alreadyRepresentative() {
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        var profile = new ProfileResponse(profileId, "닉", 20, null, null, "소개",
            26410L, "부산 북구", BigDecimal.valueOf(36.5), 0, 0,
            LocalDateTime.now().minusDays(1), LocalDateTime.now());

        var badge = Mockito.mock(Badge.class);
        given(badge.getId()).willReturn(badgeId);
        given(badge.getName()).willReturn("배지");
        given(badge.getDescription()).willReturn("설명");
        given(badge.getIconUrl()).willReturn("https://icon");
        given(badge.getCode()).willReturn("CODE");

        var pb = Mockito.mock(ProfileBadge.class);
        given(pb.isRepresentative()).willReturn(true);
        given(pb.getCreatedAt()).willReturn(LocalDateTime.now().minusDays(3));
        given(pb.getUpdatedAt()).willReturn(LocalDateTime.now().minusDays(1));

        given(profileService.getProfileByMemberId(memberId)).willReturn(profile);
        // exists & isRepresentative → true
        given(entityService.existsByProfileIdAndBadgeId(profileId, badgeId)).willReturn(true);
        given(entityService.isRepresentative(profileId, badgeId)).willReturn(true);
        // 멱등 분기에서 한 번만 단건 조회하여 DTO 매핑
        given(entityService.getByProfileIdAndBadgeId(profileId, badgeId)).willReturn(pb);
        given(badgeService.getById(badgeId)).willReturn(badge);

        var res = profileBadgeService.setRepresentativeBadge(memberId, new ProfileBadgeRepresentativeRequest(badgeId));

        assertThat(res.badgeId()).isEqualTo(badgeId);
        assertThat(res.representative()).isTrue();

        verify(entityService, never()).resetRepresentative(any());
        verify(entityService, never()).setRepresentative(any(), any());
    }

    @Test
    @DisplayName("대표 배지 변경 - exists=true & isRepresentative=false → reset → set → 재조회")
    void setRepresentative_changeFlow() {
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        var profile = new ProfileResponse(profileId, "닉", 20, null, null, "소개",
            26410L, "부산 북구", BigDecimal.valueOf(36.5), 0, 0,
            LocalDateTime.now().minusDays(1), LocalDateTime.now());

        var badge = Mockito.mock(Badge.class);
        given(badge.getId()).willReturn(badgeId);
        given(badge.getName()).willReturn("배지");
        given(badge.getDescription()).willReturn("설명");
        given(badge.getIconUrl()).willReturn("https://icon");
        given(badge.getCode()).willReturn("CODE");

        var pbAfter = Mockito.mock(ProfileBadge.class);
        given(pbAfter.isRepresentative()).willReturn(true);
        given(pbAfter.getCreatedAt()).willReturn(LocalDateTime.now().minusDays(2));
        given(pbAfter.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(profileService.getProfileByMemberId(memberId)).willReturn(profile);
        given(badgeService.getById(badgeId)).willReturn(badge);

        // exists = true, isRepresentative = false
        given(entityService.existsByProfileIdAndBadgeId(profile.id(), badgeId)).willReturn(true);
        given(entityService.isRepresentative(profile.id(), badgeId)).willReturn(false);

        // 단건 조회는 변경 후 1번만
        given(entityService.getByProfileIdAndBadgeId(profile.id(), badgeId)).willReturn(pbAfter);

        // when
        var res = profileBadgeService.setRepresentativeBadge(memberId, new ProfileBadgeRepresentativeRequest(badgeId));

        // then
        assertThat(res.badgeId()).isEqualTo(badgeId);
        assertThat(res.representative()).isTrue();

        InOrder io = inOrder(entityService);
        io.verify(entityService).existsByProfileIdAndBadgeId(profile.id(), badgeId);
        io.verify(entityService).isRepresentative(profile.id(), badgeId);
        io.verify(entityService).resetRepresentative(profile.id());
        io.verify(entityService).setRepresentative(profile.id(), badgeId);
        io.verify(entityService).getByProfileIdAndBadgeId(profile.id(), badgeId);
    }

    @Test
    @DisplayName("소유하지 않은 배지 → 예외 전파(RESET/SET 호출 안 함)")
    void setRepresentative_notOwned() {
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        var profile = new ProfileResponse(profileId, "닉", 20, null, null, "소개",
            26410L, "부산 북구", BigDecimal.valueOf(36.5), 0, 0,
            LocalDateTime.now().minusDays(1), LocalDateTime.now());
        given(profileService.getProfileByMemberId(memberId)).willReturn(profile);

        // exists=false → 바로 예외
        given(entityService.existsByProfileIdAndBadgeId(profile.id(), badgeId)).willReturn(false);

        assertThatThrownBy(() ->
            profileBadgeService.setRepresentativeBadge(memberId, new ProfileBadgeRepresentativeRequest(badgeId))
        ).isInstanceOf(NoSuchElementException.class);

        verify(entityService, never()).resetRepresentative(any());
        verify(entityService, never()).setRepresentative(any(), any());
        verify(entityService, never()).getByProfileIdAndBadgeId(any(), any());
    }
}