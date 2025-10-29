package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.repository.ProfileBadgeDslRepository;
import com.pnu.momeet.domain.badge.service.ProfileBadgeEntityService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
public class ProfileBadgeEntityServiceTest {

    @Mock
    private ProfileBadgeDslRepository profileBadgeDslRepository;

    @InjectMocks
    private ProfileBadgeEntityService entityService;

    @Test
    @DisplayName("findBadgesByProfileId - DslRepository 위임 & Pageable 전달")
    void findBadgesByProfileId() {
        UUID profileId = UUID.randomUUID();

        var rows = java.util.List.of(
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "A",
                "A",
                "https://a",
                "A",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                true
            ),
            new ProfileBadgeResponse(
                UUID.randomUUID(),
                "B",
                "B",
                "https://b",
                "B",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(2),
                false
            )
        );
        Pageable pageable = PageRequest.of(1, 3);
        Page<ProfileBadgeResponse> fake = new PageImpl<>(rows, pageable, 7);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(profileBadgeDslRepository.findBadgesByProfileId(eq(profileId), any(Pageable.class))).willReturn(fake);

        var page = entityService.findBadgesByProfileId(profileId, pageable);

        assertThat(page.getTotalElements()).isEqualTo(7);
        verify(profileBadgeDslRepository).findBadgesByProfileId(eq(profileId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("resetRepresentative - Dsl 위임")
    void resetRepresentative_delegate() {
        UUID profileId = UUID.randomUUID();
        given(profileBadgeDslRepository.resetRepresentative(profileId)).willReturn(1L);

        entityService.resetRepresentative(profileId);

        verify(profileBadgeDslRepository).resetRepresentative(profileId);
    }

    @Test
    @DisplayName("setRepresentative - 1행 이상 갱신 시 성공")
    void setRepresentative_success() {
        UUID profileId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();
        given(profileBadgeDslRepository.setRepresentative(profileId, badgeId)).willReturn(1L);

        entityService.setRepresentative(profileId, badgeId);

        verify(profileBadgeDslRepository).setRepresentative(profileId, badgeId);
    }

    @Test
    @DisplayName("setRepresentative - 0행 갱신 시 예외")
    void setRepresentative_fail_zeroUpdated() {
        UUID profileId = UUID.randomUUID();
        UUID badgeId = UUID.randomUUID();
        given(profileBadgeDslRepository.setRepresentative(profileId, badgeId)).willReturn(0L);

        assertThatThrownBy(() -> entityService.setRepresentative(profileId, badgeId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("대표 배지 설정에 실패");

        verify(profileBadgeDslRepository).setRepresentative(profileId, badgeId);
    }
}
