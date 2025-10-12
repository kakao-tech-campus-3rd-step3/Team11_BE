package com.pnu.momeet.domain.evaluation.dto.response;

import java.util.List;
import java.util.UUID;

public record EvaluationCreateBatchResponse(
    List<EvaluationResponse> created,
    List<UUID> alreadyEvaluated,
    List<InvalidItem> invalid
) {
}
