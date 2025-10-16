package com.pnu.momeet.domain.report.controller;

import com.pnu.momeet.domain.report.dto.request.ReportPageRequest;
import com.pnu.momeet.domain.report.dto.response.ReportSummaryResponse;
import com.pnu.momeet.domain.report.service.ReportDomainService;
import com.pnu.momeet.domain.report.service.mapper.ReportDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/reports")
public class ReportAdminController {

    private final ReportDomainService reportService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ReportSummaryResponse>> getOpenReports(
        @ModelAttribute ReportPageRequest pageRequest
    ) {
        PageRequest page = ReportDtoMapper.toPageRequest(pageRequest);
        return ResponseEntity.ok(reportService.getOpenReports(page));
    }
}
