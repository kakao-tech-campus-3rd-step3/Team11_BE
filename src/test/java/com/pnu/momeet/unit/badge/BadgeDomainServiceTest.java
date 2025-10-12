package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgeUpdateRequest;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgePageRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.service.BadgeDomainService;
import com.pnu.momeet.domain.badge.service.BadgeEntityService;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BadgeDomainServiceTest {

    @Mock
    private BadgeEntityService entityService;

    @Mock
    private ProfileDomainService profileService;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private SigunguEntityService sigunguService;

    @InjectMocks
    private BadgeDomainService badgeService;

    @Test
    @DisplayName("내 배지 조회 성공 - Profile 검증 후 EntityService 위임")
    void getMyBadges_success() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Sigungu sgg = sigunguService.getById(26410L);
        ProfileResponse profile = new ProfileResponse(
            profileId, "닉", 20, Gender.MALE, null, "소개", sgg.getId(),
            sgg.getSidoName() + " " + sgg.getSigunguName(),
            BigDecimal.valueOf(36.5), 0, 0,
            LocalDateTime.now().minusDays(2), LocalDateTime.now()
        );
        given(profileService.getProfileByMemberId(memberId)).willReturn(profile);

        ProfileBadgePageRequest req = new ProfileBadgePageRequest();
        req.setPage(0); req.setSize(5); req.setSort("representative,DESC,createdAt,DESC");

        var rows = java.util.List.of(
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "첫 배지",
                "첫 배지",
                "https://icon/1.png",
                "FIRST",
                LocalDateTime.now().minusDays(1),
                true
            ),
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "둘째 배지",
                "둘째 배지",
                "SECOND",
                "https://icon/2.png",
                LocalDateTime.now().minusDays(2),
                false
            )
        );
        Page<ProfileBadgeResponse> fake = new PageImpl<>(rows, PageRequest.of(0,5), 2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(entityService.findBadgesByProfileId(eq(profileId), any(Pageable.class))).willReturn(fake);

        // when
        Page<ProfileBadgeResponse> result = badgeService.getMyBadges(memberId, req);

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

        assertThatThrownBy(() -> badgeService.getMyBadges(memberId, req))
            .isInstanceOf(NoSuchElementException.class);

        verify(profileService).getProfileByMemberId(memberId);
        verify(entityService, never()).findBadgesByProfileId(any(), any());
    }

    @Test
    @DisplayName("특정 사용자 배지 조회 성공 - 프로필 존재 검증 후 EntityService 위임")
    void getUserBadges_success() {
        UUID profileId = UUID.randomUUID();
        Sigungu sgg = sigunguService.getById(26410L);
        ProfileResponse dummy = new ProfileResponse(
            profileId, "닉", 20, Gender.FEMALE, null, "소개", sgg.getId(),
            sgg.getSidoName() + " " + sgg.getSigunguName(),
            BigDecimal.valueOf(36.5), 0, 0,
            LocalDateTime.now().minusDays(3), LocalDateTime.now()
        );
        given(profileService.getProfileById(profileId)).willReturn(dummy);

        ProfileBadgePageRequest req = new ProfileBadgePageRequest();
        req.setPage(1); req.setSize(3); req.setSort("representative,DESC,createdAt,DESC");

        var rows = java.util.List.of(
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "배지A",
                "설명A",
                "A",
                "https://icon/a.png",
                LocalDateTime.now().minusHours(5),
                true
            ),
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "배지B",
                "설명B",
                "B",
                "https://icon/b.png",
                LocalDateTime.now().minusDays(1),
                false
            ),
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "배지C",
                "설명C",
                "https://icon/c.png",
                "C",
                LocalDateTime.now().minusDays(2),
                false
            )
        );
        Page<ProfileBadgeResponse> fake = new PageImpl<>(rows, PageRequest.of(1,3), 7);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(entityService.findBadgesByProfileId(eq(profileId), any(Pageable.class))).willReturn(fake);

        Page<ProfileBadgeResponse> result = badgeService.getUserBadges(profileId, req);

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

        assertThatThrownBy(() -> badgeService.getUserBadges(profileId, req))
            .isInstanceOf(NoSuchElementException.class);

        verify(profileService).getProfileById(profileId);
        verify(entityService, never()).findBadgesByProfileId(any(), any());
    }

    @Test
    @DisplayName("배지 생성 성공 - 코드 TRIM+UPPERCASE 정규화 후 저장")
    void create_success_normalizeCodeAndSave() {
        // given
        var icon = new org.springframework.mock.web.MockMultipartFile(
            "iconImage","t.png","image/png", new byte[]{1}
        );
        var req = new BadgeCreateRequest("모임 병아리", "첫 참여 배지", icon, "  first_join  ");

        // 이름/코드 모두 중복 아님
        given(entityService.existsByNameIgnoreCase("모임 병아리")).willReturn(false);
        given(entityService.existsByCodeIgnoreCase("FIRST_JOIN")).willReturn(false);

        // 업로드 URL 가짜 리턴
        given(s3StorageService.uploadImage(eq(icon), any()))
            .willReturn("https://cdn.example.com/badges/uuid.png");

        // save 시 id 세팅 흉내
        willAnswer(inv -> {
            Badge b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
            return b;
        }).given(entityService).save(any(Badge.class));

        ArgumentCaptor<Badge> badgeCaptor = ArgumentCaptor.forClass(Badge.class);

        // when
        var resp = badgeService.createBadge(req);

        // then
        assertThat(resp.name()).isEqualTo("모임 병아리");
        assertThat(resp.iconUrl()).contains("cdn.example.com/badges");

        // 저장된 엔티티의 code가 정규화되어 있는지 확인
        verify(entityService).save(badgeCaptor.capture());
        Badge saved = badgeCaptor.getValue();
        assertThat(saved.getCode()).isEqualTo("FIRST_JOIN"); // TRIM + UPPERCASE 확인 포인트

        verify(entityService).existsByNameIgnoreCase("모임 병아리");
        verify(entityService).existsByCodeIgnoreCase("FIRST_JOIN");
        verify(s3StorageService).uploadImage(eq(icon), any());
    }

    @Test
    @DisplayName("배지 생성 실패 - 이름 중복")
    void create_fail_duplicateName() {
        var icon = new org.springframework.mock.web.MockMultipartFile(
            "iconImage","t.png","image/png", new byte[]{1}
        );
        var req = new BadgeCreateRequest("중복", "desc", icon, "DUPLICATED");

        given(entityService.existsByNameIgnoreCase("중복")).willReturn(true);

        assertThatThrownBy(() -> badgeService.createBadge(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 배지");

        verify(s3StorageService, never()).uploadImage(any(), any());
        verify(entityService, never()).save(any());
    }

    @Test
    @DisplayName("배지 생성 실패 - 코드 중복 (트림+대문자 정규화 포함)")
    void create_fail_duplicateCode() {
        // given
        var icon = new org.springframework.mock.web.MockMultipartFile(
            "iconImage","t.png","image/png", new byte[]{1}
        );
        // 앞뒤 공백 + 소문자로 들어와도 내부에서 TRIM + UPPERCASE 처리
        var req = new BadgeCreateRequest("이름", "설명", icon, "  duplicate  ");

        given(entityService.existsByNameIgnoreCase("이름")).willReturn(false);
        // 정규화 결과 "DUPLICATE" 가 존재한다고 응답
        given(entityService.existsByCodeIgnoreCase("DUPLICATE")).willReturn(true);

        // when / then
        assertThatThrownBy(() -> badgeService.createBadge(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 배지 코드");

        verify(entityService).existsByNameIgnoreCase("이름");
        verify(entityService).existsByCodeIgnoreCase("DUPLICATE");
        verify(s3StorageService, never()).uploadImage(any(), any());
        verify(entityService, never()).save(any(Badge.class));
    }

    @Test
    @DisplayName("배지 수정 성공 - 이름/설명 변경 + 아이콘 교체")
    void update_success_withIconChange() {
        UUID id = UUID.randomUUID();
        Badge badge = Badge.create(
            "기존이름",
            "기존설명",
            "https://cdn.example.com/badges/old.png",
            "OLD_CODE"
        );
        ReflectionTestUtils.setField(badge, "id", id);

        given(entityService.getById(id)).willReturn(badge);
        given(entityService.existsByNameIgnoreCase("새이름")).willReturn(false);

        var newIcon = new org.springframework.mock.web.MockMultipartFile(
            "iconImage","t.png","image/png", new byte[]{1}
        );
        given(s3StorageService.uploadImage(eq(newIcon), anyString()))
            .willReturn("https://cdn.example.com/badges/new.png");
        willDoNothing().given(s3StorageService)
            .deleteImage("https://cdn.example.com/badges/old.png");

        var req = new BadgeUpdateRequest("새이름", "새설명", newIcon);

        BadgeUpdateResponse resp = badgeService.updateBadge(id, req);

        assertThat(resp.badgeId()).isEqualTo(id);
        assertThat(resp.iconUrl()).isEqualTo("https://cdn.example.com/badges/new.png");
        verify(entityService).getById(id);
        verify(s3StorageService).uploadImage(eq(newIcon), anyString());
        verify(s3StorageService).deleteImage("https://cdn.example.com/badges/old.png");
        verify(entityService, never()).save(any()); // 도메인은 dirty-checking 가정
    }

    @Test
    @DisplayName("배지 수정 성공 - 아이콘 미변경 (텍스트만)")
    void update_success_withoutIcon() {
        UUID id = UUID.randomUUID();
        Badge badge = Badge.create(
            "기존이름",
            "기존설명",
            "https://cdn.example.com/badges/exist.png",
            "OLD_CODE"
        );
        ReflectionTestUtils.setField(badge, "id", id);

        given(entityService.getById(id)).willReturn(badge);
        given(entityService.existsByNameIgnoreCase("문자수정")).willReturn(false);

        var req = new BadgeUpdateRequest("문자수정", "설명수정", null);

        BadgeUpdateResponse resp = badgeService.updateBadge(id, req);

        assertThat(resp.name()).isEqualTo("문자수정");
        assertThat(resp.iconUrl()).isEqualTo("https://cdn.example.com/badges/exist.png");
        verify(s3StorageService, never()).uploadImage(any(), anyString());
        verify(s3StorageService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("배지 수정 실패 - 대상 없음")
    void update_fail_notFound() {
        UUID id = UUID.randomUUID();
        given(entityService.getById(id)).willThrow(new NoSuchElementException("존재하지 않는 배지"));

        var req = new BadgeUpdateRequest("x", "y", null);

        assertThatThrownBy(() -> badgeService.updateBadge(id, req))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("존재하지 않는 배지");
    }

    @Test
    @DisplayName("배지 수정 실패 - 이름 중복")
    void update_fail_duplicateName() {
        UUID id = UUID.randomUUID();
        Badge badge = Badge.create(
            "원래이름",
            "기존설명",
            "https://cdn.example.com/badges/old.png",
            "OLD_CODE"
        );
        given(entityService.getById(id)).willReturn(badge);
        given(entityService.existsByNameIgnoreCase("중복이름")).willReturn(true);

        var req = new BadgeUpdateRequest("중복이름", null, null);

        assertThatThrownBy(() -> badgeService.updateBadge(id, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 배지");

        verify(s3StorageService, never()).uploadImage(any(), anyString());
        verify(s3StorageService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("배지 삭제 성공 - 엔티티 삭제 + S3 아이콘 삭제")
    void deleteBadge_success() {
        UUID id = UUID.randomUUID();
        Badge badge = Badge.create(
            "삭제대상",
            "설명",
            "https://cdn.example.com/badges/x.png",
            "DELETE"
        );
        given(entityService.getById(id)).willReturn(badge);

        badgeService.deleteBadge(id);

        verify(entityService).delete(badge);
        verify(s3StorageService).deleteImage("https://cdn.example.com/badges/x.png");
    }

    @Test
    @DisplayName("배지 삭제 실패 - 대상 없음")
    void deleteBadge_notFound() {
        UUID id = UUID.randomUUID();
        given(entityService.getById(id)).willThrow(new NoSuchElementException("존재하지 않는 배지입니다."));

        assertThatThrownBy(() -> badgeService.deleteBadge(id))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("존재하지 않는 배지");
        verify(entityService, never()).delete(any());
    }

    @Test
    @DisplayName("전체 배지 조회 - Pageable 전달 & 매핑")
    void getAllBadges_paging() {
        BadgePageRequest req = new BadgePageRequest();
        req.setPage(0); req.setSize(10); req.setSort(null);

        given(entityService.findAll(any(Pageable.class))).willReturn(Page.empty());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        var result = badgeService.getBadges(req);

        assertThat(result).isNotNull();
        verify(entityService).findAll(pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(10);
    }
}