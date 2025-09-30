package com.pnu.momeet.domain.badge.service;

import com.pnu.momeet.domain.badge.enums.BadgeRule;
import com.pnu.momeet.domain.badge.enums.Metric;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BadgeRuleService {

    private final ProfileEntityService profileService;

    public List<String> evaluateOnMeetupFinished(UUID profileId) {
        int joins = profileService.getById(profileId).getCompletedJoinMeetups();

        return EnumSet.allOf(BadgeRule.class).stream()
            .filter(rule -> rule.getMetric() == Metric.JOIN_COUNT)
            .filter(rule -> joins == rule.getThreshold())
            .map(BadgeRule::getCode)
            .distinct()
            .toList();
    }

    public List<String> evaluateOnEvaluationSubmitted(EvaluationSubmittedEvent e) {
        // 받은 좋아요 누적 기준
        if (!Rating.LIKE.equals(e.getRating())) {
            return List.of();
        }
        int likeCount = profileService.getById(e.getTargetProfileId()).getLikes();

        return EnumSet.allOf(BadgeRule.class).stream()
            .filter(rule -> rule.getMetric() == Metric.LIKE_COUNT)
            .filter(rule -> likeCount == rule.getThreshold())
            .map(BadgeRule::getCode)
            .distinct()
            .toList();
    }
}
