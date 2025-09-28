package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BadgeRuleEngineImpl implements BadgeRuleEngine {

    private final ProfileEntityService profileService;

    @Override
    public List<String> evaluateOnMeetupFinished(UUID profileId) {
        int joins = profileService.getById(profileId).getCompletedJoinMeetups();

        return EnumSet.allOf(BadgeRule.class).stream()
            .filter(rule -> rule.metric() == Metric.JOIN_COUNT)
            .filter(rule -> joins == rule.threshold())
            .map(BadgeRule::code)
            .distinct()
            .toList();
    }

    @Override
    public List<String> evaluateOnEvaluationSubmitted(EvaluationSubmittedEvent e) {
        // 받은 좋아요 누적 기준
        if (!Rating.LIKE.equals(e.rating())) {
            return List.of();
        }
        int likeCount = profileService.getById(e.targetProfileId()).getLikes();

        return EnumSet.allOf(BadgeRule.class).stream()
            .filter(rule -> rule.metric() == Metric.LIKE_COUNT)
            .filter(rule -> likeCount == rule.threshold())
            .map(BadgeRule::code)
            .distinct()
            .toList();
    }
}
