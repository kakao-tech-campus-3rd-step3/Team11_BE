package com.pnu.momeet.domain.meetup.service;

import com.pnu.momeet.domain.evaluation.service.EvaluationService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetupEvaluationFacade {
    private final EvaluationService evaluationService;
    private final ProfileService profileSerivice;

    public List<EvaluatableProfileResponse> getEvaluatableUsers(UUID meetupId, UUID evaluatorMemberId) {
        Profile profile = profileSerivice.getProfileEntityByMemberId(evaluatorMemberId);
        return evaluationService.getEvaluatableUsers(meetupId, profile.getId());
    }
}
