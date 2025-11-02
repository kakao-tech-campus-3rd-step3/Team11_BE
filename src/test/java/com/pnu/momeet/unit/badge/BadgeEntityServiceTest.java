package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.repository.ProfileBadgeDslRepository;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.badge.service.BadgeEntityService;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import org.springframework.data.domain.Sort;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BadgeEntityServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @InjectMocks
    private BadgeEntityService entityService;

    @Test
    @DisplayName("getById - 존재하면 반환")
    void getById_exists() {
        UUID id = UUID.randomUUID();
        Badge badge = Badge.create("이름", "설명", "https://icon", "CODE");
        given(badgeRepository.findById(id)).willReturn(Optional.of(badge));

        Badge found = entityService.getById(id);

        assertThat(found).isNotNull();
        verify(badgeRepository).findById(id);
    }

    @Test
    @DisplayName("getById - 미존재시 NoSuchElementException")
    void getById_notExists() {
        UUID id = UUID.randomUUID();
        given(badgeRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> entityService.getById(id))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("existsByNameIgnoreCase - 위임")
    void existsByName() {
        given(badgeRepository.existsByNameIgnoreCase("dup")).willReturn(true);
        assertThat(entityService.existsByNameIgnoreCase("dup")).isTrue();
        verify(badgeRepository).existsByNameIgnoreCase("dup");
    }

    @Test
    @DisplayName("save - 위임")
    void save() {
        Badge b = Badge.create("n", "d", "https://icon", "CODE");
        given(badgeRepository.save(b)).willReturn(b);

        var saved = entityService.save(b);

        assertThat(saved).isSameAs(b);
        verify(badgeRepository).save(b);
    }

    @Test
    @DisplayName("delete - 위임")
    void delete() {
        Badge b = Badge.create("n", "d", "https://icon", "CODE");
        entityService.delete(b);
        verify(badgeRepository).delete(b);
    }

    @Test
    @DisplayName("findAll - Pageable 전달")
    void findAll() {
        Pageable pageable = PageRequest.of(
            0, 10, Sort.by(Sort.Order.desc("createdAt"))
        );
        given(badgeRepository.findAll(pageable)).willReturn(Page.empty());

        var page = entityService.findAll(pageable);

        assertThat(page.getTotalElements()).isZero();
        verify(badgeRepository).findAll(pageable);
    }

    @Test
    @DisplayName("existsByCodeIgnoreCase - 위임(true)")
    void existsByCodeIgnoreCase_true() {
        // given
        given(badgeRepository.existsByCodeIgnoreCase("FIRST_JOIN")).willReturn(true);

        // when / then
        assertThat(entityService.existsByCodeIgnoreCase("FIRST_JOIN")).isTrue();
        verify(badgeRepository).existsByCodeIgnoreCase("FIRST_JOIN");
    }

    @Test
    @DisplayName("existsByCodeIgnoreCase - 위임(false)")
    void existsByCodeIgnoreCase_false() {
        // given
        given(badgeRepository.existsByCodeIgnoreCase("UNIQUE_CODE")).willReturn(false);

        // when / then
        assertThat(entityService.existsByCodeIgnoreCase("UNIQUE_CODE")).isFalse();
        verify(badgeRepository).existsByCodeIgnoreCase("UNIQUE_CODE");
    }
}