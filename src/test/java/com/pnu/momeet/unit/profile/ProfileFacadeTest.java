package com.pnu.momeet.unit.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.evaluation.service.EvaluationService;
import com.pnu.momeet.domain.meetup.dto.response.UnEvaluatedMeetupDto;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import com.pnu.momeet.domain.profile.service.ProfileFacade;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ProfileFacadeTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private MeetupService meetupService;

    @Mock
    private EvaluationService evaluationService;

    @InjectMocks
    private ProfileFacade profileFacade;

    @Test
    @DisplayName("평가하지 않은 모임 조회 성공 - Builder 기반 Meetup 생성")
    void getUnEvaluatedMeetups_success()
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // given
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        // 테스트용 Profile (owner + evaluator)
        Profile me = Profile.create(
            memberId,
            "테스터",
            25,
            Gender.MALE,
            null,
            "테스트 소개",
            "부산"
        );
        ReflectionTestUtils.setField(me, "id", profileId);

        // 테스트용 Sigungu
        Constructor<Sigungu> constructor = Sigungu.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Sigungu sigungu = constructor.newInstance();

        ReflectionTestUtils.setField(sigungu, "id", 26410L);
        ReflectionTestUtils.setField(sigungu, "sidoCode", 26L);
        ReflectionTestUtils.setField(sigungu, "sidoName", "부산광역시");
        ReflectionTestUtils.setField(sigungu, "sigunguName", "금정구");

        // 테스트용 Meetup (Builder 사용)
        Meetup meetup = Meetup.builder()
            .owner(me)
            .name("종료된 테스트 모임")
            .category(MainCategory.SPORTS)
            .subCategory(SubCategory.SOCCER)
            .description("테스트용 모임 설명")
            .capacity(10)
            .scoreLimit(3.0)
            .locationPoint(new GeometryFactory().createPoint(new Coordinate(129.059, 35.153)))
            .address("부산 서면 ○○카페")
            .sigungu(sigungu)
            .endAt(LocalDateTime.now().minusDays(1)) // 이미 종료된 모임
            .build();
        ReflectionTestUtils.setField(meetup, "id", UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Meetup> meetups = new PageImpl<>(List.of(meetup), pageable, 1);

        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.of(me));
        given(meetupService.findEndedMeetupsByProfileId(profileId, pageable)).willReturn(meetups);
        given(evaluationService.calculateUnEvaluatedCount(meetup, profileId)).willReturn(2L);

        // when
        Page<UnEvaluatedMeetupDto> result = profileFacade.getUnEvaluatedMeetups(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        UnEvaluatedMeetupDto dto = result.getContent().get(0);
        assertThat(dto.name()).isEqualTo("종료된 테스트 모임");
        assertThat(dto.unEvaluatedCount()).isEqualTo(2L);
        assertThat(dto.capacity()).isEqualTo(10);

        verify(profileRepository).findByMemberId(memberId);
        verify(meetupService).findEndedMeetupsByProfileId(profileId, pageable);
        verify(evaluationService).calculateUnEvaluatedCount(meetup, profileId);
    }

    @Test
    @DisplayName("평가하지 않은 모임 조회 실패 - 프로필 없음")
    void getUnEvaluatedMeetups_fail_profileNotFound() {
        UUID memberId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> profileFacade.getUnEvaluatedMeetups(memberId, pageable));

        verify(profileRepository).findByMemberId(memberId);
    }
}
