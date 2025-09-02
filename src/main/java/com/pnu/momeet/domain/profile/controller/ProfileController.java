package com.pnu.momeet.domain.profile.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.profile.dto.ProfileResponse;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(profileService.getMyProfile(userDetails.getMemberId()));
    }
}
