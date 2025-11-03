package com.pnu.momeet.unit.meetup;

import static org.mockito.Mockito.when;

import com.pnu.momeet.domain.meetup.dto.request.MeetupPageRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
import com.pnu.momeet.domain.meetup.service.MeetupEntityService;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.lang.reflect.Constructor;
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
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private ProfileEntityService profileService;

    private static Profile profileWithId(UUID id) {
        try {
            Constructor<Profile> ctor = Profile.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Profile p = ctor.newInstance();
            ReflectionTestUtils.setField(p, "id", id);
            return p;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("페이지 조회: DB에서 필터된 결과를 사용하므로 content/total이 일치(빈 페이지)")
    void getAllBySpecification_returnsEmptyPage_consistentTotals() {
        // given
        UUID viewerMemberId = UUID.randomUUID();
        UUID viewerProfileId = UUID.randomUUID();
        MeetupPageRequest req = new MeetupPageRequest();

        when(profileService.getByMemberId(viewerMemberId))
            .thenReturn(profileWithId(viewerProfileId));

        Page<Meetup> repoPage =
            new PageImpl<>(java.util.Collections.emptyList(),
                PageRequest.of(0, 10), 0);

        when(meetupEntityService.getAllBySpecificationWithPagination(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(Pageable.class)
        )).thenReturn(repoPage);

        // when
        Page<MeetupResponse> result =
            meetupDomainService.getAllBySpecification(req, viewerMemberId);

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
        UUID viewerMemberId = UUID.randomUUID();
        UUID viewerProfileId = UUID.randomUUID();

        when(profileService.getByMemberId(viewerMemberId))
            .thenReturn(profileWithId(viewerProfileId));

        when(meetupEntityService.isBlockedInMeetup(meetupId, viewerProfileId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> meetupDomainService.getById(meetupId, viewerMemberId))
            .isInstanceOf(NoSuchElementException.class);

        verify(meetupEntityService, never()).getByIdWithDetails(any());
    }

    @Test
    @DisplayName("상세 조회: 차단이 아니면 엔티티 재조회 후 매핑하여 반환")
    void getById_notBlocked_thenDelegates() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID viewerMemberId = UUID.randomUUID();
        UUID viewerProfileId = UUID.randomUUID();

        when(profileService.getByMemberId(viewerMemberId))
            .thenReturn(profileWithId(viewerProfileId));

        when(meetupEntityService.isBlockedInMeetup(meetupId, viewerProfileId)).thenReturn(false);
        Meetup entity = mock(Meetup.class);
        when(meetupEntityService.getByIdWithDetails(meetupId)).thenReturn(entity);

        try (MockedStatic<MeetupEntityMapper> mocked = mockStatic(MeetupEntityMapper.class)) {
            mocked.when(() -> MeetupEntityMapper.toDetail(entity))
                .thenReturn(mock(MeetupDetail.class));

            // when
            MeetupDetail detail = meetupDomainService.getById(meetupId, viewerMemberId);

            // then
            verify(meetupEntityService, times(1)).getByIdWithDetails(meetupId);
            assertThat(detail).isNotNull();
        }
    }
}