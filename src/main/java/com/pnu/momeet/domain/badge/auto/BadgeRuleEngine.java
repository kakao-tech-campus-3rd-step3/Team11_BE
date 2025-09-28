package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import java.util.UUID;
import java.util.stream.Stream;

public interface BadgeRuleEngine {
    Stream<String> evaluateOnMeetupFinished(UUID profileId);
    Stream<String> evaluateOnEvaluationSubmitted(EvaluationSubmittedEvent e);
}
