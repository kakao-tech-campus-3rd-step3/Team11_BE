package com.pnu.momeet.domain.profile.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.profile.dto.request.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.request.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileDomainService profileService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(profileService.getMyProfile(userDetails.getMemberId()));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable UUID profileId) {
        return ResponseEntity.ok(profileService.getProfileById(profileId));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ProfileResponse> createMyProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute ProfileCreateRequest request
    ) {
        ProfileResponse response = profileService.createMyProfile(userDetails.getMemberId(), request);
        return ResponseEntity
            .created(URI.create("/api/profiles/me"))
            .body(response);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PatchMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute ProfileUpdateRequest request
    ) {
        ProfileResponse response = profileService.updateMyProfile(userDetails.getMemberId(), request);
        return ResponseEntity.ok(response);
    }
}
