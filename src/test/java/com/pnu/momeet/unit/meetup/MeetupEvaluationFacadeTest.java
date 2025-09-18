package com.pnu.momeet.unit.meetup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.service.EvaluationService;
import com.pnu.momeet.domain.meetup.service.MeetupEvaluationFacade;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MeetupEvaluationFacadeTest {

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private MeetupEvaluationFacade meetupEvaluationFacade;

    @Test
    @DisplayName("평가 가능한 사용자 조회 성공")
    void getEvaluatableUsers_success() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile evaluatorProfile = Profile.create(
            memberId,
            "평가자",
            25,
            Gender.MALE,
            "https://example.com/me.png",
            "소개",
            "서울"
        );
        ReflectionTestUtils.setField(evaluatorProfile, "id", profileId);

        EvaluatableProfileResponse response = new EvaluatableProfileResponse(
            UUID.randomUUID(),
            "참가자",
            "https://example.com/p.png",
            BigDecimal.valueOf(36.5),
            Rating.LIKE
        );

        given(profileService.getProfileEntityByMemberId(memberId))
            .willReturn(evaluatorProfile);
        given(evaluationService.getEvaluatableUsers(meetupId, profileId))
            .willReturn(List.of(response));

        // when
        List<EvaluatableProfileResponse> result =
            meetupEvaluationFacade.getEvaluatableUsers(meetupId, memberId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().nickname()).isEqualTo("참가자");
        assertThat(result.getFirst().currentEvaluation()).isEqualTo(Rating.LIKE);

        verify(profileService).getProfileEntityByMemberId(memberId);
        verify(evaluationService).getEvaluatableUsers(meetupId, profileId);
    }

    @Test
    @DisplayName("평가 가능한 사용자 조회 실패 - 프로필 없음")
    void getEvaluatableUsers_fail_profileNotFound() {
        UUID meetupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        given(profileService.getProfileEntityByMemberId(memberId))
            .willThrow(new NoSuchElementException("프로필 없음"));

        assertThrows(NoSuchElementException.class,
            () -> meetupEvaluationFacade.getEvaluatableUsers(meetupId, memberId));

        verify(profileService).getProfileEntityByMemberId(memberId);
        verifyNoInteractions(evaluationService);
    }
}
