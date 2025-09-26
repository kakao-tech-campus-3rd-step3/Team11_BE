package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BadgeRuleEngineImpl implements BadgeRuleEngine {

    private static final int FIRST_COUNT = 1;
    private static final int TEN_COUNT = 10;

    private static final String FIRST_JOIN = "FIRST_JOIN";
    private static final String TEN_JOINS = "TEN_JOINS";
    private static final String LIKE_10 = "LIKE_10";

    private final ProfileEntityService profileService;

    @Override
    public Stream<String> evaluateOnMeetupFinished(UUID profileId) {
        int joins = profileService.getById(profileId).getCompletedJoinMeetups();

        return EnumSet.allOf(BadgeRule.class).stream()
            .filter(rule -> rule.metric() == Metric.JOIN_COUNT)
            .filter(rule -> joins == rule.threshold())
            .map(BadgeRule::code);
    }

    @Override
    public Stream<String> evaluateOnEvaluationSubmitted(EvaluationSubmittedEvent e) {
        // 받은 좋아요 누적 기준
        if (!"LIKE".equalsIgnoreCase(e.rating())) {
            return Stream.empty();
        }
        int likeCount = profileService.getById(e.targetProfileId()).getLikes();

        return EnumSet.allOf(BadgeRule.class).stream()
            .filter(rule -> rule.metric() == Metric.LIKE_COUNT)
            .filter(rule -> likeCount == rule.threshold())
            .map(BadgeRule::code);
    }
}
