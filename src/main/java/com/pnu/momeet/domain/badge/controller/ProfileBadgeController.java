package com.pnu.momeet.domain.badge.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgeUpdateRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileBadgeController {

    private final BadgeDomainService badgeService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me/badges")
    public Page<ProfileBadgeResponse> getMyBadges(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute ProfileBadgePageRequest request
    ) {
        return badgeService.getMyBadges(userDetails.getMemberId(), request);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{profileId}/badges")
    public Page<ProfileBadgeResponse> getUserBadges(
        @PathVariable UUID profileId,
        @Valid @ModelAttribute ProfileBadgePageRequest request
    ) {
        return badgeService.getUserBadges(profileId, request);
    }
}
