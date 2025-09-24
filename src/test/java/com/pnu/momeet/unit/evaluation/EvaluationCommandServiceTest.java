package com.pnu.momeet.unit.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateRequest;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;
import com.pnu.momeet.domain.evaluation.service.EvaluationCommandService;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.service.ParticipantDomainService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.ProfileService;
import com.pnu.momeet.unit.BaseUnitTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EvaluationCommandServiceTest extends BaseUnitTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private ParticipantDomainService participantService;

    @InjectMocks
    private EvaluationCommandService evaluationService;

    @Test
    @DisplayName("평가 성공 - LIKE")
    void createEvaluation_success_like() {
        UUID evaluatorMemberId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetMemberId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();
        UUID meetupId = UUID.randomUUID();

        Profile evaluator = Profile.create(
            evaluatorMemberId,
            "평가자",
            25, Gender.MALE,
            null,
            null,
            "부산"
        );
        ReflectionTestUtils.setField(evaluator, "id", evaluatorProfileId);

        Profile target = Profile.create(
            targetMemberId,
            "대상자",
            30,
            Gender.FEMALE,
            null,
            null,
            "서울"
        );
        ReflectionTestUtils.setField(target, "id", targetProfileId);

        given(profileService.getProfileEntityByMemberId(evaluatorMemberId)).willReturn(evaluator);
        given(profileService.getProfileEntityByProfileId(targetProfileId)).willReturn(target);

        given(evaluationRepository.existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
            eq(meetupId), eq(evaluatorProfileId), eq(targetProfileId))
        ).willReturn(false);

        given(evaluationRepository.existsByMeetupIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
            eq(meetupId), eq(targetProfileId), eq("fakeHash"), any(LocalDateTime.class))
        ).willReturn(false);

        given(evaluationRepository.save(any(Evaluation.class))).willAnswer(inv -> inv.getArgument(0));

        EvaluationCreateRequest request = new EvaluationCreateRequest(meetupId, targetProfileId, Rating.LIKE);

        EvaluationResponse resp = evaluationService.createEvaluation(evaluatorMemberId, request, "fakeHash");

        assertThat(resp.rating()).isEqualTo("LIKE");
        verify(evaluationRepository).save(any(Evaluation.class));
    }

    @Test
    @DisplayName("실패 - 동일 모임에서 중복 평가 불가")
    void createEvaluation_fail_duplicate() {
        UUID evaluatorMemberId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetMemberId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();
        UUID meetupId = UUID.randomUUID();

        Profile evaluator = Profile.create(
            evaluatorMemberId,
            "평가자",
            25,
            Gender.MALE,
            null,
            null,
            "부산"
        );
        ReflectionTestUtils.setField(evaluator, "id", evaluatorProfileId);

        Profile target = Profile.create(
            targetMemberId,
            "대상자",
            30,
            Gender.FEMALE,
            null,
            null,
            "서울"
        );
        ReflectionTestUtils.setField(target, "id", targetProfileId);

        given(profileService.getProfileEntityByMemberId(evaluatorMemberId)).willReturn(evaluator);
        given(profileService.getProfileEntityByProfileId(targetProfileId)).willReturn(target);

        // 동일 모임에서 evaluator → target 이미 평가한 기록 있다고 가정
        given(evaluationRepository.existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
            eq(meetupId), eq(evaluatorProfileId), eq(targetProfileId))
        ).willReturn(true);

        EvaluationCreateRequest request = new EvaluationCreateRequest(meetupId, targetProfileId, Rating.LIKE);

        assertThrows(IllegalStateException.class,
            () -> evaluationService.createEvaluation(evaluatorMemberId, request, "fakeHash"));
    }

    @Test
    @DisplayName("실패 - 동일 대상자 쿨타임 내 재평가 불가")
    void createEvaluation_fail_cooltime() {
        UUID evaluatorMemberId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetMemberId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();
        UUID meetupId = UUID.randomUUID();

        Profile evaluator = Profile.create(
            evaluatorMemberId,
            "평가자",
            25,
            Gender.MALE,
            null,
            null,
            "부산"
        );
        ReflectionTestUtils.setField(evaluator, "id", evaluatorProfileId);

        Profile target = Profile.create(
            targetMemberId,
            "대상자",
            30,
            Gender.FEMALE,
            null,
            null,
            "서울"
        );
        ReflectionTestUtils.setField(target, "id", targetProfileId);

        given(profileService.getProfileEntityByMemberId(evaluatorMemberId)).willReturn(evaluator);
        given(profileService.getProfileEntityByProfileId(targetProfileId)).willReturn(target);

        // 최근 평가가 존재하고, 쿨타임 내라고 가정
        Evaluation lastEval = Evaluation.create(
            meetupId,
            evaluatorProfileId,
            targetProfileId,
            Rating.LIKE,
            "fakeHash"
        );
        ReflectionTestUtils.setField(lastEval, "createdAt", LocalDateTime.now());

        given(evaluationRepository.findTopByEvaluatorProfileIdAndTargetProfileIdOrderByCreatedAtDesc(
            eq(evaluatorProfileId), eq(targetProfileId))
        ).willReturn(Optional.of(lastEval));

        EvaluationCreateRequest request = new EvaluationCreateRequest(meetupId, targetProfileId, Rating.LIKE);

        assertThrows(IllegalStateException.class,
            () -> evaluationService.createEvaluation(evaluatorMemberId, request, "fakeHash"));
    }

    @Test
    @DisplayName("실패 - 동일 위치(IP 해시) 다계정 평가 방지")
    void createEvaluation_fail_sameIpHash() {
        UUID evaluatorMemberId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetMemberId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();
        UUID meetupId = UUID.randomUUID();

        Profile evaluator = Profile.create(
            evaluatorMemberId,
            "평가자",
            25,
            Gender.MALE,
            null,
            null,
            "부산"
        );
        ReflectionTestUtils.setField(evaluator, "id", evaluatorProfileId);

        Profile target = Profile.create(
            targetMemberId,
            "대상자",
            30,
            Gender.FEMALE,
            null,
            null,
            "서울"
        );
        ReflectionTestUtils.setField(target, "id", targetProfileId);

        given(profileService.getProfileEntityByMemberId(evaluatorMemberId)).willReturn(evaluator);
        given(profileService.getProfileEntityByProfileId(targetProfileId)).willReturn(target);

        given(evaluationRepository.existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
            eq(meetupId), eq(evaluatorProfileId), eq(targetProfileId))
        ).willReturn(false);

        // 동일 IP에서 이미 평가했다고 가정
        given(evaluationRepository.existsByMeetupIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
            eq(meetupId), eq(targetProfileId), eq("fakeHash"), any(LocalDateTime.class))
        ).willReturn(true);

        EvaluationCreateRequest request = new EvaluationCreateRequest(meetupId, targetProfileId, Rating.LIKE);

        assertThrows(IllegalStateException.class,
            () -> evaluationService.createEvaluation(evaluatorMemberId, request, "fakeHash"));
    }

    @Test
    @DisplayName("평가 가능한 사용자 조회 성공 - 자기 자신 제외, 평가 없음")
    void getEvaluatableUsers_success_noEvaluations() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();

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

        given(participantService.getParticipantsByMeetupId(meetupId))
            .willReturn(List.of(
                new ParticipantResponse(
                    1L,
                    new ProfileResponse(
                        evaluatorProfileId,
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

        given(evaluationRepository.findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorProfileId))
            .willReturn(List.of());

        // when
        List<EvaluatableProfileResponse> result =
            evaluationService.getEvaluatableUsers(meetupId, evaluatorProfileId);

        // then
        assertThat(result).hasSize(1); // 자기 자신 제외
        EvaluatableProfileResponse resp = result.getFirst();
        assertThat(resp.profileId()).isEqualTo(targetProfileId);
        assertThat(resp.nickname()).isEqualTo("참가자");
        assertThat(resp.currentEvaluation()).isNull();
    }

    @Test
    @DisplayName("평가 가능한 사용자 조회 성공 - 이미 평가한 경우 LIKE 반환")
    void getEvaluatableUsers_success_withExistingEvaluation() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();

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
            evaluationService.getEvaluatableUsers(meetupId, evaluatorProfileId);

        // then
        assertThat(result).hasSize(1);
        EvaluatableProfileResponse resp = result.getFirst();
        assertThat(resp.currentEvaluation()).isEqualTo(Rating.LIKE);
    }
}
