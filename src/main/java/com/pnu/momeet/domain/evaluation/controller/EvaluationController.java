package com.pnu.momeet.domain.evaluation.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.common.util.IpHashUtil;
import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateRequest;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.service.EvaluationQueryService;
import com.pnu.momeet.domain.evaluation.service.EvaluationCommandService;
import com.pnu.momeet.domain.meetup.dto.response.UnEvaluatedMeetupDto;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationCommandService evaluationCommandService;
    private final EvaluationQueryService evaluationQueryService;
    private final IpHashUtil ipHashUtil;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<EvaluationResponse> createEvaluation(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody EvaluationCreateRequest request,
        HttpServletRequest httpServletRequest
    ) {
        String ipHash = ipHashUtil.fromRequest(httpServletRequest);
        EvaluationResponse response = evaluationCommandService.createEvaluation(
            userDetails.getMemberId(),
            request,
            ipHash
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{meetupId}/evaluatable-users")
    public ResponseEntity<List<EvaluatableProfileResponse>> getEvaluatableUsers(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID meetupId
    ) {
        return ResponseEntity.ok(
            evaluationQueryService.getEvaluatableUsers(meetupId, userDetails.getMemberId())
        );
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me/unevaluated-meetups")
    public ResponseEntity<Page<UnEvaluatedMeetupDto>> getUnEvaluatedMeetups(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<UnEvaluatedMeetupDto> result =
            evaluationQueryService.getUnEvaluatedMeetups(userDetails.getMemberId(), pageable);

        return ResponseEntity.ok(result);
    }
}
