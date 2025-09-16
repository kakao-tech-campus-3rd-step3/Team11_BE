package com.pnu.momeet.domain.evaluation.dto.request;

import com.pnu.momeet.domain.evaluation.enums.Rating;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EvaluationCreateRequest(

    @NotNull(message = "모임 ID는 필수입니다.")
    UUID meetupId,

    @NotNull(message = "평가할 프로필 ID는 필수입니다.")
    UUID targetProfileId,

    @NotNull(message = "평가 타입은 필수입니다.")
    Rating rating
) {
}