package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.util.ArrayList;
import java.util.List;
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
        List<String> out = new ArrayList<>();
        if (joins == FIRST_COUNT) out.add(FIRST_JOIN);
        if (joins == TEN_COUNT)   out.add(TEN_JOINS);

        return out.stream();
    }

    @Override
    public Stream<String> evaluateOnEvaluationSubmitted(EvaluationSubmittedEvent e) {
        // 받은 좋아요 누적 기준
        if ("LIKE".equalsIgnoreCase(e.rating())) {
            int likes = profileService.getById(e.targetProfileId()).getLikes();
            if (likes == TEN_COUNT) return Stream.of(LIKE_10);
        }
        return Stream.empty();
    }
}
