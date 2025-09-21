package com.pnu.momeet.domain.badge.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgeUpdateRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.service.BadgeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me/badges")
    public Page<BadgeResponse> getMyBadges(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute BadgePageRequest request
    ) {
        return badgeService.getMyBadges(userDetails.getMemberId(), request);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{profileId}/badges")
    public Page<BadgeResponse> getUserBadges(
        @PathVariable UUID profileId,
        @Valid @ModelAttribute BadgePageRequest request
    ) {
        return badgeService.getUserBadges(profileId, request);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/badges")
    public ResponseEntity<BadgeCreateResponse> createBadge(
        @Valid @ModelAttribute BadgeCreateRequest request
    ) {
        BadgeCreateResponse saved = badgeService.createBadge(request);
        URI location = URI.create("/api/profiles/badges/" + saved.badgeId());
        return ResponseEntity.created(location).body(saved);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PatchMapping("/badges/{badgeId}")
    public ResponseEntity<BadgeUpdateResponse> updateBadge(
        @PathVariable UUID badgeId,
        @Valid @ModelAttribute BadgeUpdateRequest request
    ) {
        BadgeUpdateResponse saved = badgeService.updateBadge(badgeId, request);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/badges/{badgeId}")
    public void deleteBadge(
        @PathVariable UUID badgeId
    ) {
        badgeService.deleteBadge(badgeId);
    }
}
