package com.pnu.momeet.domain.evaluation.dto.request;

import jakarta.validation.Valid;
import java.util.List;

public record EvaluationCreateBatchRequest(
    List<@Valid EvaluationCreateRequest> items
) {
}
