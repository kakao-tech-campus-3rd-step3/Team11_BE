package com.pnu.momeet.common.mapper.facade;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EvaluatableProfileMapper {
    public static EvaluatableProfileResponse toEvaluatableProfileResponse(
        Participant participant, Evaluation evaluation
    ) {
        Profile profile = participant.getProfile();
        return new EvaluatableProfileResponse(
            profile.getId(),
            profile.getNickname(),
            profile.getImageUrl(),
            profile.getTemperature(),
            evaluation != null ? evaluation.getRating() : null
        );
    }
}
