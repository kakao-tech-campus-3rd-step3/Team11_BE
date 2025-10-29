package com.pnu.momeet.domain.badge.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgeRepresentativeRequest;
import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.service.ProfileBadgeDomainService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileBadgeController {

    private final ProfileBadgeDomainService profileBadgeService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me/badges")
    public Page<ProfileBadgeResponse> getMyBadges(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute ProfileBadgePageRequest request
    ) {
        return profileBadgeService.getMyBadges(userDetails.getMemberId(), request);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{profileId}/badges")
    public Page<ProfileBadgeResponse> getUserBadges(
        @PathVariable UUID profileId,
        @Valid @ModelAttribute ProfileBadgePageRequest request
    ) {
        return profileBadgeService.getUserBadges(profileId, request);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/me/badges/representative")
    public ResponseEntity<ProfileBadgeResponse> setRepresentative(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ProfileBadgeRepresentativeRequest request
    ) {
        return ResponseEntity.ok(profileBadgeService.setRepresentativeBadge(userDetails.getMemberId(), request));
    }
}
