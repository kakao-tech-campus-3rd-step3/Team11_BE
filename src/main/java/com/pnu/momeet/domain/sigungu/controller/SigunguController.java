package com.pnu.momeet.domain.sigungu.controller;

import com.pnu.momeet.domain.sigungu.dto.PointWithInRequest;
import com.pnu.momeet.domain.sigungu.dto.SigunguPageRequest;
import com.pnu.momeet.domain.sigungu.dto.SigunguResponse;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.mapper.DtoMapper;
import com.pnu.momeet.domain.sigungu.mapper.EntityMapper;
import com.pnu.momeet.domain.sigungu.service.SigunguService;
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
    private final SigunguService sigunguService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<SigunguResponse>> sigunguList(
        @Valid @ModelAttribute SigunguPageRequest request
    ) {
        Page<Sigungu> sigunguPage;
        if (request.getSidoCode() == null) {
            sigunguPage = sigunguService.findAll(DtoMapper.toPageRequest(request));
        } else {
            sigunguPage = sigunguService.findAllBySidoCode(
                    String.valueOf(request.getSidoCode()),
                    DtoMapper.toPageRequest(request)
            );
        }
        return ResponseEntity.ok(sigunguPage.map(EntityMapper::toDto));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<SigunguResponse> sigunguInfo(
        @PathVariable Long id
    ) {
        Sigungu sigungu = sigunguService.findById(id);
        return ResponseEntity.ok(EntityMapper.toDto(sigungu));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/code/{sigunguCode}")
    public ResponseEntity<SigunguResponse> sigunguInfoByCode(
        @PathVariable String  sigunguCode
    ) {
        Sigungu sigungu = sigunguService.findBySigunguCode(sigunguCode);
        return ResponseEntity.ok(EntityMapper.toDto(sigungu));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/within")
    public ResponseEntity<SigunguResponse> sigunguInfoByLocation(
        @Valid @ModelAttribute PointWithInRequest request
    ) {
        Sigungu sigungu = sigunguService.findByPointIn(request.latitude(), request.longitude());
        return ResponseEntity.ok(EntityMapper.toDto(sigungu));
    }
}
