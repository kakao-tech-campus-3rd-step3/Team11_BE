package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import java.util.List;
import java.util.UUID;

public interface BadgeRuleEngine {
    List<String> evaluateOnMeetupFinished(UUID profileId);
    List<String> evaluateOnEvaluationSubmitted(EvaluationSubmittedEvent e);
}
