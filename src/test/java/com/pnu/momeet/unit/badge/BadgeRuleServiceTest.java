package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.badge.service.BadgeRuleService;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.time.LocalDateTime;
import java.util.List;
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
class BadgeRuleServiceTest {

    @Mock
    ProfileEntityService profileService;

    @InjectMocks
    BadgeRuleService ruleEngine;

    @Test
    @DisplayName("모임 종료 평가 - 첫 참여 시 FIRST_JOIN 배지 코드 반환")
    void evaluateOnMeetupFinished_firstJoin() {
        UUID pid = UUID.randomUUID();
        Profile p = mock(Profile.class);
        given(profileService.getById(pid)).willReturn(p);
        given(p.getCompletedJoinMeetups()).willReturn(1);

        List<String> codes = ruleEngine.evaluateOnMeetupFinished(pid);

        assertThat(codes).containsExactly("FIRST_JOIN");
        verify(profileService).getById(pid);
        verify(p).getCompletedJoinMeetups();
    }

    @Test
    @DisplayName("모임 종료 평가 - 10회 참여 시 TEN_JOINS 배지 코드 반환")
    void evaluateOnMeetupFinished_tenJoins() {
        UUID pid = UUID.randomUUID();
        Profile p = mock(Profile.class);
        given(profileService.getById(pid)).willReturn(p);
        given(p.getCompletedJoinMeetups()).willReturn(10);

        List<String> codes = ruleEngine.evaluateOnMeetupFinished(pid);

        assertThat(codes).containsExactly("TEN_JOINS");
    }

    @Test
    @DisplayName("모임 종료 평가 - 해당 없음 시 빈 결과")
    void evaluateOnMeetupFinished_none() {
        UUID pid = UUID.randomUUID();
        Profile p = mock(Profile.class);
        given(profileService.getById(pid)).willReturn(p);
        given(p.getCompletedJoinMeetups()).willReturn(7);

        List<String> codes = ruleEngine.evaluateOnMeetupFinished(pid);

        assertThat(codes).isEmpty();
    }

    @Test
    @DisplayName("평가 제출 - LIKE + 누적 10개일 때 LIKE_10 발급")
    void evaluateOnEvaluationSubmitted_like10() {
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now();
        EvaluationSubmittedEvent e = new EvaluationSubmittedEvent(
            meetupId,
            evaluatorProfileId,
            targetProfileId,
            Rating.LIKE
        );

        Profile p = mock(Profile.class);
        given(profileService.getById(targetProfileId)).willReturn(p);
        given(p.getLikes()).willReturn(10);

        List<String> codes = ruleEngine.evaluateOnEvaluationSubmitted(e);

        assertThat(codes).containsExactly("LIKE_10");
    }

    @Test
    @DisplayName("평가 제출 - LIKE가 아니거나 10개가 아닐 때 미발급")
    void evaluateOnEvaluationSubmitted_notLikeOrNotTen() {
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorProfileId = UUID.randomUUID();
        UUID targetProfileId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now();

        // DISLIKE 일 때
        EvaluationSubmittedEvent e1 = new EvaluationSubmittedEvent(
            meetupId,
            evaluatorProfileId,
            targetProfileId,
            Rating.DISLIKE
        );

        assertThat(ruleEngine.evaluateOnEvaluationSubmitted(e1)).isEmpty();

        // LIKE 이지만 누적 9
        EvaluationSubmittedEvent e2 = new EvaluationSubmittedEvent(
            meetupId,
            evaluatorProfileId,
            targetProfileId,
            Rating.LIKE
        );
        Profile p = mock(Profile.class);
        given(profileService.getById(targetProfileId)).willReturn(p);
        given(p.getLikes()).willReturn(9);
        assertThat(ruleEngine.evaluateOnEvaluationSubmitted(e2)).isEmpty();
    }
}
