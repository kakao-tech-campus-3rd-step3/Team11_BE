package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pnu.momeet.domain.badge.service.BadgeAwardService;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.badge.repository.ProfileBadgeRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BadgeAwardServiceTest {

    @Mock
    BadgeRepository badgeRepository;

    @Mock
    ProfileBadgeRepository profileBadgeRepository;

    @InjectMocks
    BadgeAwardService awarder;

    @Test
    @DisplayName("award - 코드 정규화 후 매핑·저장 호출")
    void award_success_normalizeAndSave() {
        UUID profileId = UUID.randomUUID();
        UUID badgeId   = UUID.randomUUID();

        // 엔티티 반환으로 변경된 저장소 스텁 (Badge mock 으로 getId()만 제공)
        Badge badge = mock(Badge.class);
        when(badge.getId()).thenReturn(badgeId);
        given(badgeRepository.findByCodeIgnoreCase("FIRST_JOIN"))
            .willReturn(Optional.of(badge));

        awarder.award(profileId, "  first_join  ");

        // 코드 정규화 후 엔티티 조회가 호출되었는지
        verify(badgeRepository).findByCodeIgnoreCase("FIRST_JOIN");

        // ProfileBadge 저장 호출 검증
        verify(profileBadgeRepository).save(argThat(pb ->
            pb.getProfileId().equals(profileId) &&
                pb.getBadgeId().equals(badgeId)));
    }

    @Test
    @DisplayName("award - 매핑 누락 시 예외 발생, 저장 호출 없음")
    void award_fail_missingMapping() {
        UUID profileId = UUID.randomUUID();

        given(badgeRepository.findByCodeIgnoreCase("UNKNOWN"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> awarder.award(profileId, "unknown"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("배지 코드"); // 메시지 키워드는 구현 메시지에 맞게 완화

        verify(profileBadgeRepository, never()).save(any());
    }
}