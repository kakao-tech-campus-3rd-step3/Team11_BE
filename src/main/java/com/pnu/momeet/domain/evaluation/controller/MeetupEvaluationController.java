package com.pnu.momeet.domain.evaluation.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.common.util.IpHashUtil;
import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateBatchRequest;
import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateRequest;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationCreateBatchResponse;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.service.EvaluationDomainService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetups")
public class MeetupEvaluationController {

    private final EvaluationDomainService evaluationService;
    private final IpHashUtil ipHashUtil;

    @GetMapping("/{meetupId}/evaluations/candidates")
    public ResponseEntity<List<EvaluatableProfileResponse>> getEvaluatableUsers(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID meetupId
    ) {
        return ResponseEntity.ok(
            evaluationService.getEvaluatableUsers(meetupId, userDetails.getMemberId())
        );
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/{meetupId}/evaluations")
    public ResponseEntity<EvaluationCreateBatchResponse> createEvaluations(
        @PathVariable UUID meetupId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody EvaluationCreateBatchRequest request,
        HttpServletRequest httpServletRequest
    ) {
        String ipHash = ipHashUtil.fromRequest(httpServletRequest);
        EvaluationCreateBatchResponse response = evaluationService.createEvaluations(
            userDetails.getMemberId(),
            meetupId,
            request,
            ipHash
        );
        return ResponseEntity.ok(response);
    }
}
