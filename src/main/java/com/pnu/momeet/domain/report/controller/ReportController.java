package com.pnu.momeet.domain.report.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.common.util.IpHashUtil;
import com.pnu.momeet.domain.report.dto.request.ReportCreateRequest;
import com.pnu.momeet.domain.report.dto.response.ReportResponse;
import com.pnu.momeet.domain.report.service.ReportDomainService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportDomainService reportService;
    private final IpHashUtil ipHashUtil;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping()
    public ResponseEntity<ReportResponse> createReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute ReportCreateRequest reportCreateRequest,
        HttpServletRequest httpServletRequest
    ) {
        String ipHash = ipHashUtil.fromRequest(httpServletRequest);
        ReportResponse saved = reportService.createReport(
            userDetails.getMemberId(),
            reportCreateRequest,
            ipHash
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
