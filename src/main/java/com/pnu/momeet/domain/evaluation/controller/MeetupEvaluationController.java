package com.pnu.momeet.domain.evaluation.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.evaluation.service.EvaluationDomainService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetups")
public class MeetupEvaluationController {

    private final EvaluationDomainService evaluationService;

    @GetMapping("/{meetupId}/evaluations/candidates")
    public ResponseEntity<List<EvaluatableProfileResponse>> getEvaluatableUsers(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID meetupId
    ) {
        return ResponseEntity.ok(
            evaluationService.getEvaluatableUsers(meetupId, userDetails.getMemberId())
        );
    }
}
