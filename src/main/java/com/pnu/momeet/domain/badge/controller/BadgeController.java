package com.pnu.momeet.domain.badge.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.service.BadgeService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
