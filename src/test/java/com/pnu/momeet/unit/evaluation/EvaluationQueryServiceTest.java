package com.pnu.momeet.unit.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;
import com.pnu.momeet.domain.evaluation.service.EvaluationQueryService;
import com.pnu.momeet.domain.meetup.dto.response.UnEvaluatedMeetupDto;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.service.ParticipantDomainService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.ProfileService;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.unit.BaseUnitTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
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
@ExtendWith(MockitoExtension.class)
public class EvaluationQueryServiceTest extends BaseUnitTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private ParticipantDomainService participantService;

    @Mock
    private ProfileService profileService;

    @Mock
    private MeetupDomainService meetupService;

    @InjectMocks
    private EvaluationQueryService evaluationQueryService;

    @Test
    @DisplayName("평가 가능한 사용자 조회 성공 - 자기 자신 제외, 평가 없음")
    void getEvaluatableUsers_success_noEvaluations() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorMemberId = UUID.randomUUID();  // ← 멤버 ID (서비스 내부에서 프로필로 변환)
        UUID evaluatorProfileId = UUID.randomUUID(); // ← 프로필 ID
        UUID targetProfileId = UUID.randomUUID();

        // evaluator 프로필 엔티티 스텁 (서비스 내부에서 사용)
        Profile evaluatorProfile = Profile.create(
            evaluatorMemberId,
            "평가자",
            30,
            Gender.MALE,
            null,
            "소개",
            "부산"
        );
        ReflectionTestUtils.setField(evaluatorProfile, "id", evaluatorProfileId);
        given(profileService.getProfileEntityByMemberId(evaluatorMemberId))
            .willReturn(evaluatorProfile);

        ProfileResponse targetProfile = new ProfileResponse(
            targetProfileId,
            "참가자",
            25,
            Gender.MALE,
            "https://example.com/test.png",
            "소개",
            "서울",
            BigDecimal.valueOf(36.5),
            10,
            2,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        ParticipantResponse participantResponse = new ParticipantResponse(
            1L,
            targetProfile,
            "MEMBER",
            true,
            false,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now().minusDays(1)
        );

        // 모임 참가자: evaluator + target (자기 자신 제외 검증)
        given(participantService.getParticipantsByMeetupId(meetupId))
            .willReturn(List.of(
                new ParticipantResponse(
                    999L,
                    new ProfileResponse(
                        evaluatorProfileId, // evaluator의 프로필 ID
                        "평가자",
                        30,
                        Gender.MALE,
                        null,
                        "소개",
                        "부산",
                        BigDecimal.valueOf(37.0),
                        5,
                        1,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                    ),
                    "MEMBER",
                    true,
                    false,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ),
                participantResponse
            ));

        // 기존 평가 없음
        given(evaluationRepository.findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorProfileId))
            .willReturn(List.of());

        // when
        List<EvaluatableProfileResponse> result =
            evaluationQueryService.getEvaluatableUsers(meetupId, evaluatorMemberId);

        // then
        assertThat(result).hasSize(1); // 자기 자신 제외
        EvaluatableProfileResponse resp = result.getFirst();
        assertThat(resp.profileId()).isEqualTo(targetProfileId);
        assertThat(resp.nickname()).isEqualTo("참가자");
        assertThat(resp.currentEvaluation()).isNull();

        verify(profileService).getProfileEntityByMemberId(evaluatorMemberId);
        verify(participantService).getParticipantsByMeetupId(meetupId);
        verify(evaluationRepository).findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorProfileId);
    }

    @Test
    @DisplayName("평가 가능한 사용자 조회 성공 - 이미 평가한 경우 LIKE 반환")
    void getEvaluatableUsers_success_withExistingEvaluation() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorMemberId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();

        // evaluator 프로필 스텁
        Profile evaluatorProfile = Profile.create(
            evaluatorMemberId,
            "평가자",
            29,
            Gender.FEMALE,
            null,
            "소개",
            "부산"
        );
        ReflectionTestUtils.setField(evaluatorProfile, "id", evaluatorProfileId);
        given(profileService.getProfileEntityByMemberId(evaluatorMemberId))
            .willReturn(evaluatorProfile);

        ProfileResponse targetProfile = new ProfileResponse(
            targetProfileId,
            "참가자",
            25,
            Gender.FEMALE,
            "https://example.com/test.png",
            "소개",
            "서울",
            BigDecimal.valueOf(36.5),
            10,
            2,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        ParticipantResponse participantResponse = new ParticipantResponse(
            1L,
            targetProfile,
            "MEMBER",
            true,
            false,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now().minusDays(1)
        );

        given(participantService.getParticipantsByMeetupId(meetupId))
            .willReturn(List.of(participantResponse));

        Evaluation existingEval = Evaluation.create(
            meetupId,
            evaluatorProfileId,
            targetProfileId,
            Rating.LIKE,
            "ipHash"
        );
        given(evaluationRepository.findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorProfileId))
            .willReturn(List.of(existingEval));

        // when
        List<EvaluatableProfileResponse> result =
            evaluationQueryService.getEvaluatableUsers(meetupId, evaluatorMemberId);

        // then
        assertThat(result).hasSize(1);
        EvaluatableProfileResponse resp = result.getFirst();
        assertThat(resp.currentEvaluation()).isEqualTo(Rating.LIKE);

        verify(profileService).getProfileEntityByMemberId(evaluatorMemberId);
        verify(evaluationRepository).findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorProfileId);
    }

    @Test
    @DisplayName("평가하지 않은 모임 조회 성공 - Builder 기반 Meetup 생성")
    void getUnEvaluatedMeetups_success()
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // given
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        // 프로필 엔티티 (owner + evaluator)
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

        // Sigungu 엔티티
        Constructor<Sigungu> constructor = Sigungu.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Sigungu sigungu = constructor.newInstance();
        ReflectionTestUtils.setField(sigungu, "id", 26410L);
        ReflectionTestUtils.setField(sigungu, "sidoCode", 26L);
        ReflectionTestUtils.setField(sigungu, "sidoName", "부산광역시");
        ReflectionTestUtils.setField(sigungu, "sigunguName", "금정구");

        // Meetup 엔티티 (Builder)
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

        // unEvaluatedCount = participantCount - 1 - evaluatedCount
        // 원하는 값(2)을 맞추기 위해 participantCount=3, evaluatedCount=0으로 고정
        ReflectionTestUtils.setField(meetup, "participantCount", 3);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Meetup> meetups = new PageImpl<>(List.of(meetup), pageable, 1);

        // 스텁: 프로필 조회, 종료된 모임 조회, 평가 개수 카운트
        given(profileService.getProfileEntityByMemberId(memberId)).willReturn(me);
        given(meetupService.findEndedMeetupsByProfileId(profileId, pageable)).willReturn(meetups);
        given(evaluationRepository.countByMeetupIdAndEvaluatorProfileId(meetup.getId(), profileId))
            .willReturn(0L);

        // when
        Page<UnEvaluatedMeetupDto> result = evaluationQueryService.getUnEvaluatedMeetups(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        UnEvaluatedMeetupDto dto = result.getContent().getFirst();
        assertThat(dto.name()).isEqualTo("종료된 테스트 모임");
        assertThat(dto.unEvaluatedCount()).isEqualTo(2L);
        assertThat(dto.capacity()).isEqualTo(10);

        verify(profileService).getProfileEntityByMemberId(memberId);
        verify(meetupService).findEndedMeetupsByProfileId(profileId, pageable);
        verify(evaluationRepository).countByMeetupIdAndEvaluatorProfileId(meetup.getId(), profileId);
    }

    @Test
    @DisplayName("평가하지 않은 모임 조회 실패 - 프로필 없음")
    void getUnEvaluatedMeetups_fail_profileNotFound() {
        UUID memberId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        given(profileService.getProfileEntityByMemberId(memberId))
            .willThrow(new NoSuchElementException("프로필이 존재하지 않습니다."));

        assertThrows(NoSuchElementException.class,
            () -> evaluationQueryService.getUnEvaluatedMeetups(memberId, pageable));

        verify(profileService).getProfileEntityByMemberId(memberId);
    }
}