package com.pnu.momeet.domain.evaluation.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.evaluation.service.EvaluationDomainService;
import com.pnu.momeet.domain.meetup.dto.request.MeetupSummaryPageRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profiles")
public class ProfileEvaluationController {

    private final EvaluationDomainService evaluationService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me/meetups")
    public ResponseEntity<Page<MeetupSummaryResponse>> getMyMeetups(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(name = "evaluated", required = false) Boolean evaluated,
        @Valid @ModelAttribute MeetupSummaryPageRequest request
    ) {
        if (Boolean.TRUE.equals(evaluated)) {
            return ResponseEntity.ok(evaluationService.getMyEvaluatedMeetups(userDetails.getMemberId(), request));
        }
        if (Boolean.FALSE.equals(evaluated)) {
            return ResponseEntity.ok(evaluationService.getMyUnEvaluatedMeetups(userDetails.getMemberId(), request));
        }
        return ResponseEntity.ok(evaluationService.getMyRecentMeetupsMixed(userDetails.getMemberId(), request));
    }
}
