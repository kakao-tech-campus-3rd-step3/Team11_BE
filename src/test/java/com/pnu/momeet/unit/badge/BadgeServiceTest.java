package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.repository.BadgeDslRepository;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.badge.service.BadgeService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private BadgeDslRepository badgeDslRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    @DisplayName("내 배지 조회 성공 - ProfileService/Repository 위임 및 페이지 변환 검증")
    void getMyBadges_success() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile real = Profile.create(memberId, "닉", 20, Gender.MALE, null, "소개", "부산");
        Profile profile = Mockito.spy(real);
        Mockito.doReturn(profileId).when(profile).getId();

        given(profileService.getProfileEntityByMemberId(memberId)).willReturn(profile);

        given(profileService.getProfileEntityByMemberId(memberId)).willReturn(profile);

        BadgePageRequest req = new BadgePageRequest();
        req.setPage(0);
        req.setSize(5);
        req.setSort("representative,DESC,createdAt,DESC");

        // 레포가 반환할 페이크 페이지
        List<BadgeResponse> rows = List.of(
            new BadgeResponse(
                UUID.randomUUID(),
                "첫 배지",
                "첫 배지",
                "https://icon/1.png",
                LocalDateTime.now().minusDays(1),
                true
            ),
            new BadgeResponse(
                UUID.randomUUID(),
                "둘째 배지",
                "둘째 배지",
                "https://icon/2.png",
                LocalDateTime.now().minusDays(2),
                false
            )
        );
        Page<BadgeResponse> fakePage = new PageImpl<>(rows, PageRequest.of(0,5), 2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(badgeDslRepository.findBadgesByProfileId(eq(profileId), any(Pageable.class)))
            .willReturn(fakePage);

        // when
        Page<BadgeResponse> result = badgeService.getMyBadges(memberId, req);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        verify(profileService).getProfileEntityByMemberId(memberId);

        verify(badgeDslRepository).findBadgesByProfileId(eq(profileId), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort().isSorted()).isTrue();
    }

    @Test
    @DisplayName("내 배지 조회 실패 - 프로필이 없으면 NoSuchElementException 전파")
    void getMyBadges_fail_profileNotFound() {
        // given
        UUID memberId = UUID.randomUUID();
        BadgePageRequest req = new BadgePageRequest();
        req.setPage(0); req.setSize(10);

        given(profileService.getProfileEntityByMemberId(memberId))
            .willThrow(new NoSuchElementException("프로필이 존재하지 않습니다."));

        // when & then
        assertThrows(NoSuchElementException.class,
            () -> badgeService.getMyBadges(memberId, req));

        verify(profileService).getProfileEntityByMemberId(memberId);
        verify(badgeDslRepository, never()).findBadgesByProfileId(any(), any());
    }

    @Test
    @DisplayName("특정 사용자 배지 조회 성공 - 프로필 존재 검증 후 Repository 위임 및 페이지 변환 검증")
    void getUserBadges_success() {
        // given
        UUID profileId = UUID.randomUUID();

        // 존재 검증만 통과하면 되므로, 반환값은 내용이 중요치 않음
        UUID dummyMemberId = UUID.randomUUID();
        Profile dummy = Profile.create(
            dummyMemberId,
            "닉",
            20,
            Gender.FEMALE,
            null,
            "소개",
            "부산"
        );
        given(profileService.getProfileEntityByProfileId(profileId)).willReturn(dummy);

        BadgePageRequest req = new BadgePageRequest();
        req.setPage(1);
        req.setSize(3);
        req.setSort("representative,DESC,createdAt,DESC");

        List<BadgeResponse> rows = List.of(
            new BadgeResponse(
                UUID.randomUUID(),
                "배지A",
                "설명A",
                "https://icon/a.png",
                LocalDateTime.now().minusHours(5),
                true
            ),
            new BadgeResponse(
                UUID.randomUUID(),
                "배지B",
                "설명B",
                "https://icon/b.png",
                LocalDateTime.now().minusDays(1),
                false
            ),
            new BadgeResponse(
                UUID.randomUUID(),
                "배지C",
                "설명C",
                "https://icon/c.png",
                LocalDateTime.now().minusDays(2),
                false
            )
        );
        Page<BadgeResponse> fakePage = new PageImpl<>(
            rows, PageRequest.of(1, 3), 7
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(badgeDslRepository.findBadgesByProfileId(eq(profileId), any(Pageable.class)))
            .willReturn(fakePage);

        // when
        Page<BadgeResponse> result = badgeService.getUserBadges(profileId, req);

        // then
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getContent()).hasSize(3);

        verify(profileService).getProfileEntityByProfileId(profileId);
        verify(badgeDslRepository).findBadgesByProfileId(eq(profileId), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(3);
        assertThat(used.getSort().isSorted()).isTrue();
    }

    @Test
    @DisplayName("특정 사용자 배지 조회 실패 - 프로필 미존재 시 NoSuchElementException 전파")
    void getUserBadges_fail_profileNotFound() {
        // given
        UUID profileId = UUID.randomUUID();
        BadgePageRequest req = new BadgePageRequest();
        req.setPage(0);
        req.setSize(10);

        given(profileService.getProfileEntityByProfileId(profileId))
            .willThrow(new NoSuchElementException("프로필이 존재하지 않습니다."));

        // when & then
        assertThrows(NoSuchElementException.class,
            () -> badgeService.getUserBadges(profileId, req));

        verify(profileService).getProfileEntityByProfileId(profileId);
        verify(badgeDslRepository, never()).findBadgesByProfileId(any(), any());
    }

    @Test
    @DisplayName("성공 - 새 배지 생성 + S3 업로드 호출")
    void create_success() {
        // given
        MockMultipartFile icon = new MockMultipartFile(
            "iconImage",
            "t.png",
            "image/png",
            new byte[]{1}
        );
        BadgeCreateRequest req = new BadgeCreateRequest(
            "모임 병아리", "첫 참여 배지", icon
        );

        given(badgeRepository.existsByNameIgnoreCase("모임 병아리")).willReturn(false);
        given(s3StorageService.uploadImage(eq(icon), any())).willReturn("https://cdn.example.com/badges/uuid.png");
        willAnswer(invocation -> {
            Badge b = invocation.getArgument(0);
            // JPA가 해줄 일을 테스트에서 시뮬레이션
            ReflectionTestUtils.setField(b, "id", java.util.UUID.randomUUID());
            return b;
        }).given(badgeRepository).save(any(Badge.class));

        // when
        BadgeCreateResponse resp = badgeService.createBadge(req);

        // then
        assertThat(resp.name()).isEqualTo("모임 병아리");
        assertThat(resp.iconUrl()).contains("cdn.example.com/badges");
        assertThat(resp.badgeId()).isNotNull();
        // Repository.save 로 전달된 엔티티 필드 검증
        ArgumentCaptor<Badge> captor = ArgumentCaptor.forClass(Badge.class);
        verify(badgeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("모임 병아리");
        assertThat(captor.getValue().getDescription()).isEqualTo("첫 참여 배지");
        assertThat(captor.getValue().getIconUrl()).contains("cdn.example.com/badges");

        verify(s3StorageService, times(1)).uploadImage(eq(icon), any());
    }

    @Test
    @DisplayName("실패 - 이름 중복이면 IllegalArgumentException")
    void create_fail_duplicateName() {
        // given
        MockMultipartFile icon = new MockMultipartFile("iconImage", "t.png", "image/png", new byte[]{1});
        BadgeCreateRequest req = new BadgeCreateRequest("중복", "desc", icon);

        given(badgeRepository.existsByNameIgnoreCase("중복")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> badgeService.createBadge(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 배지 이름");

        verify(badgeRepository).existsByNameIgnoreCase("중복");
        verify(badgeRepository, never()).save(any());
        verify(s3StorageService, never()).uploadImage(any(), any());
    }
}