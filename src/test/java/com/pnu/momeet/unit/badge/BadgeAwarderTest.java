package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.badge.auto.BadgeAwarder;
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
class BadgeAwarderTest {

    @Mock
    BadgeRepository badgeRepository;

    @Mock
    ProfileBadgeRepository profileBadgeRepository;

    @InjectMocks
    BadgeAwarder awarder;

    @Test
    @DisplayName("award - 코드 정규화 후 매핑·저장 호출")
    void award_success_normalizeAndInsert() {
        UUID profileId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();

        given(badgeRepository.findIdByCode("FIRST_JOIN")).willReturn(Optional.of(badgeId));

        awarder.award(profileId, "  first_join  ");

        verify(badgeRepository).findIdByCode("FIRST_JOIN");
        verify(profileBadgeRepository).insertIgnore(profileId, badgeId);
    }

    @Test
    @DisplayName("award - 매핑 누락 시 예외 발생, 저장 호출 없음")
    void award_fail_missingMapping() {
        UUID profileId = UUID.randomUUID();
        given(badgeRepository.findIdByCode("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> awarder.award(profileId, "unknown"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("배지 코드 매핑 누락");

        verify(profileBadgeRepository, never()).insertIgnore(any(), any());
    }
}
