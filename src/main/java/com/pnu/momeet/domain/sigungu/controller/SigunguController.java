package com.pnu.momeet.domain.sigungu.controller;

import com.pnu.momeet.domain.sigungu.dto.request.PointWithInRequest;
import com.pnu.momeet.domain.sigungu.dto.request.SigunguPageRequest;
import com.pnu.momeet.domain.sigungu.dto.response.SigunguResponse;
import com.pnu.momeet.domain.sigungu.service.SigunguDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sigungu")
@RequiredArgsConstructor
public class SigunguController {
    private final SigunguDomainService sigunguService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<SigunguResponse>> sigunguList(
        @Valid @ModelAttribute SigunguPageRequest request
    ) {
        Page<SigunguResponse> sigunguPage = sigunguService.findAllWithSidoCode(request);
        return ResponseEntity.ok(sigunguPage);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<SigunguResponse> sigunguInfo(
        @PathVariable Long id
    ) {
        var sigunguResponse = sigunguService.getById(id);
        return ResponseEntity.ok(sigunguResponse);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/within")
    public ResponseEntity<SigunguResponse> sigunguInfoByLocation(
        @Valid @ModelAttribute PointWithInRequest request
    ) {
        var sigunguResponse = sigunguService.getByPointIn(request.latitude(), request.longitude());
        return ResponseEntity.ok(sigunguResponse);
    }
}
