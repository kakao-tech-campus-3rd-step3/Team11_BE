package com.pnu.momeet.unit.meetup;

import static org.mockito.Mockito.when;

import com.pnu.momeet.domain.meetup.dto.request.MeetupPageRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
import com.pnu.momeet.domain.meetup.service.MeetupEntityService;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetupDomainServiceTest {

    @InjectMocks
    private MeetupDomainService meetupDomainService;

    @Mock
    private MeetupEntityService meetupEntityService;

    @Test
    @DisplayName("페이지 조회: DB에서 필터된 결과를 사용하므로 content/total이 일치(빈 페이지)")
    void getAllBySpecification_returnsEmptyPage_consistentTotals() {
        // given
        UUID viewer = UUID.randomUUID();
        MeetupPageRequest req = new MeetupPageRequest();

        Page<Meetup> repoPage =
            new PageImpl<Meetup>(java.util.Collections.emptyList(),
                PageRequest.of(0, 10), 0);

        when(meetupEntityService.getAllBySpecificationWithPagination(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(Pageable.class)
        )).thenReturn(repoPage);

        // when
        Page<MeetupResponse> result =
            meetupDomainService.getAllBySpecification(req, viewer);

        // then
        verify(meetupEntityService, times(1)).getAllBySpecificationWithPagination(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(Pageable.class)
        );
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("상세 조회: viewer가 차단 관계라면 404로 위장(Not Found)")
    void getById_blocked_then404() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        when(meetupEntityService.isBlockedInMeetup(meetupId, viewer)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> meetupDomainService.getById(meetupId, viewer))
            .isInstanceOf(NoSuchElementException.class);

        verify(meetupEntityService, never()).getByIdWithDetails(any());
    }

    @Test
    @DisplayName("상세 조회: 차단이 아니면 엔티티 재조회 후 매핑하여 반환")
    void getById_notBlocked_thenDelegates() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        when(meetupEntityService.isBlockedInMeetup(meetupId, viewer)).thenReturn(false);
        Meetup entity = mock(Meetup.class);
        when(meetupEntityService.getByIdWithDetails(meetupId)).thenReturn(entity);

        try (MockedStatic<MeetupEntityMapper> mocked = mockStatic(MeetupEntityMapper.class)) {
            mocked.when(() -> MeetupEntityMapper.toDetail(entity))
                .thenReturn(mock(MeetupDetail.class));

            // when
            MeetupDetail detail = meetupDomainService.getById(meetupId, viewer);

            // then
            verify(meetupEntityService, times(1)).getByIdWithDetails(meetupId);
            assertThat(detail).isNotNull();
        }
    }
}