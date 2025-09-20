package com.pnu.momeet.domain.evaluation.service.mapper;

import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EvaluationEntityMapper {

    public static EvaluationResponse toResponseDto(Evaluation evaluation) {
        return new EvaluationResponse(
            evaluation.getId(),
            evaluation.getMeetupId(),
            evaluation.getEvaluatorProfileId(),
            evaluation.getTargetProfileId(),
            evaluation.getRating().name(),
            evaluation.getCreatedAt()
        );
    }
}
