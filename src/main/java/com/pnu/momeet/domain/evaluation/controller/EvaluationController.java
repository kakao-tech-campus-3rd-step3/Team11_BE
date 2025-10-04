package com.pnu.momeet.domain.evaluation.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.common.util.IpHashUtil;
import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateRequest;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.service.EvaluationDomainService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationDomainService evaluationService;
    private final IpHashUtil ipHashUtil;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<EvaluationResponse> createEvaluation(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody EvaluationCreateRequest request,
        HttpServletRequest httpServletRequest
    ) {
        String ipHash = ipHashUtil.fromRequest(httpServletRequest);
        EvaluationResponse response = evaluationService.createEvaluation(
            userDetails.getMemberId(),
            request,
            ipHash
        );
        return ResponseEntity.ok(response);
    }
}
