package com.pnu.momeet.unit.badge;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.badge.auto.BadgeAutoGrantListener;
import com.pnu.momeet.domain.badge.auto.BadgeAwarder;
import com.pnu.momeet.domain.badge.auto.BadgeRuleEngine;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BadgeAutoGrantListenerTest {

    @Mock
    BadgeRuleEngine badgeRuleEngine;

    @Mock
    BadgeAwarder badgeAwarder;

    @InjectMocks
    BadgeAutoGrantListener listener;

    @Test
    @DisplayName("모임 종료 이벤트 - 모든 참가자에 대해 평가 코드 부여")
    void handleMeetupFinishedEvent_awardsAllParticipants() {
        UUID meetupId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        MeetupFinishedEvent e = new MeetupFinishedEvent(meetupId, List.of(p1, p2), LocalDateTime.now());

        given(badgeRuleEngine.evaluateOnMeetupFinished(p1)).willReturn(
            Stream.of("FIRST_JOIN", "TEN_JOINS"));
        given(badgeRuleEngine.evaluateOnMeetupFinished(p2)).willReturn(Stream.of());

        listener.handleMeetupFinishedEvent(e);

        // p1은 2개 코드 부여
        verify(badgeAwarder).award(p1, "FIRST_JOIN");
        verify(badgeAwarder).award(p1, "TEN_JOINS");
        // p2는 없음
        verify(badgeAwarder, never()).award(eq(p2), anyString());
    }

    @Test
    @DisplayName("평가 제출 이벤트 - 대상자에게 평가 코드 부여")
    void handleEvaluationSubmitted_awardsTarget() {
        UUID meetupId = UUID.randomUUID();
        UUID evaluator = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        EvaluationSubmittedEvent e = new EvaluationSubmittedEvent(meetupId, evaluator, target, "LIKE", LocalDateTime.now());

        given(badgeRuleEngine.evaluateOnEvaluationSubmitted(e)).willReturn(Stream.of("LIKE_10"));

        listener.handleEvaluationSubmitted(e);

        verify(badgeAwarder).award(target, "LIKE_10");
    }
}
