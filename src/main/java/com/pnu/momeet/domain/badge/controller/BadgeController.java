package com.pnu.momeet.domain.badge.controller;

import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgeUpdateRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.service.BadgeDomainService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeDomainService badgeService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping()
    public ResponseEntity<BadgeCreateResponse> createBadge(
        @Valid @ModelAttribute BadgeCreateRequest request
    ) {
        BadgeCreateResponse saved = badgeService.createBadge(request);
        URI location = URI.create("/api/profiles/badges/" + saved.badgeId());
        return ResponseEntity.created(location).body(saved);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping()
    public Page<BadgeResponse> getBadges(
        @Valid @ModelAttribute BadgePageRequest request
    ) {
        return badgeService.getBadges(request);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PatchMapping("/{badgeId}")
    public ResponseEntity<BadgeUpdateResponse> updateBadge(
        @PathVariable UUID badgeId,
        @Valid @ModelAttribute BadgeUpdateRequest request
    ) {
        BadgeUpdateResponse saved = badgeService.updateBadge(badgeId, request);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{badgeId}")
    public void deleteBadge(
        @PathVariable UUID badgeId
    ) {
        badgeService.deleteBadge(badgeId);
    }
}
