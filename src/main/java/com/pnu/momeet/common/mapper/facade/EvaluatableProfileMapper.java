package com.pnu.momeet.common.mapper.facade;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EvaluatableProfileMapper {
    public static EvaluatableProfileResponse toEvaluatableProfileResponse(
        ParticipantResponse participant, Evaluation evaluation
    ) {
        ProfileResponse profile = participant.profile();
        return new EvaluatableProfileResponse(
            profile.id(),
            profile.nickname(),
            profile.imageUrl(),
            profile.temperature(),
            evaluation != null ? evaluation.getRating() : null
        );
    }
}
