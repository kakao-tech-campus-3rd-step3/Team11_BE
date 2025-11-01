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
import com.pnu.momeet.common.tx.AfterCommitExecutor;
import com.pnu.momeet.common.util.ImageHashUtil;
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
import java.util.function.Consumer;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BadgeDomainServiceTest {

    @Mock
    private BadgeEntityService entityService;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private ImageHashUtil imageHashUtil;

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    @InjectMocks
    private BadgeDomainService badgeService;

    // updateBadge(badge, consumer)가 실제로 badge에 consumer를 적용하고 badge를 반환하도록 모킹
    private void stubUpdateBadgeApplyAndReturn() {
        willAnswer(inv -> {
            Badge b = inv.getArgument(0);
            @SuppressWarnings("unchecked")
            Consumer<Badge> applier = inv.getArgument(1);
            applier.accept(b);
            return b;
        }).given(entityService).updateBadge(any(Badge.class), any());
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
        Badge badge = Badge.create("기존이름","기존설명","https://cdn.example.com/badges/old.png","OLD_CODE");
        ReflectionTestUtils.setField(badge, "id", id);

        given(entityService.getById(id)).willReturn(badge);
        given(entityService.existsByNameIgnoreCaseAndIdNot("새이름", id)).willReturn(false);
        stubUpdateBadgeApplyAndReturn(); // updateBadge 모킹

        var newIcon = new MockMultipartFile("iconImage","t.png","image/png", new byte[]{1,2,3});
        given(imageHashUtil.sha256Hex(newIcon)).willReturn("HASH_NEW");
        given(s3StorageService.uploadImage(eq(newIcon), anyString()))
            .willReturn("https://cdn.example.com/badges/new.png");

        var req = new BadgeUpdateRequest("새이름","새설명", newIcon);

        var resp = badgeService.updateBadge(id, req);

        assertThat(resp.badgeId()).isEqualTo(id);
        assertThat(resp.iconUrl()).isEqualTo("https://cdn.example.com/badges/new.png");
        verify(s3StorageService).uploadImage(eq(newIcon), anyString());
        // deleteImage는 afterCommit 에서 호출되므로, 단위 테스트에서는 호출 유무 검증을 생략하거나 별도 훅으로 검증
    }

    @Test
    @DisplayName("배지 수정 - 동일 아이콘 업로드 시 업로드/삭제 미호출(NO-OP)")
    void update_skip_sameIcon() {
        UUID id = UUID.randomUUID();
        Badge badge = Badge.create("기존이름","기존설명","https://cdn.example.com/badges/old.png","CODE");
        ReflectionTestUtils.setField(badge, "id", id);
        ReflectionTestUtils.setField(badge, "iconHash", "HASH_SAME");
        given(entityService.getById(id)).willReturn(badge);

        // 불필요한 이름중복 스텁 제거 (호출되지 않음)
        var sameIcon = new MockMultipartFile("iconImage","x.png","image/png", new byte[]{9,9,9});
        given(imageHashUtil.sha256Hex(sameIcon)).willReturn("HASH_SAME");

        var req = new BadgeUpdateRequest("기존이름","기존설명", sameIcon);

        var resp = badgeService.updateBadge(id, req);

        assertThat(resp.iconUrl()).isEqualTo("https://cdn.example.com/badges/old.png");
        verify(s3StorageService, never()).uploadImage(any(), anyString());
        verify(s3StorageService, never()).deleteImage(anyString());
    }


    @Test
    @DisplayName("배지 수정 성공 - 아이콘 미변경 (텍스트만)")
    void update_success_withoutIcon() {
        UUID id = UUID.randomUUID();
        Badge badge = Badge.create("기존이름","기존설명","https://cdn.example.com/badges/exist.png","OLD_CODE");
        ReflectionTestUtils.setField(badge, "id", id);

        given(entityService.getById(id)).willReturn(badge);
        given(entityService.existsByNameIgnoreCaseAndIdNot("문자수정", id)).willReturn(false);
        stubUpdateBadgeApplyAndReturn();

        var req = new BadgeUpdateRequest("문자수정", "설명수정", null);

        var resp = badgeService.updateBadge(id, req);

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
        Badge badge = Badge.create("원래이름","기존설명","https://cdn.example.com/badges/old.png","OLD_CODE");
        ReflectionTestUtils.setField(badge, "id", id); // id 세팅

        given(entityService.getById(id)).willReturn(badge);
        given(entityService.existsByNameIgnoreCaseAndIdNot("중복이름", id)).willReturn(true); // 자기자신 제외

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