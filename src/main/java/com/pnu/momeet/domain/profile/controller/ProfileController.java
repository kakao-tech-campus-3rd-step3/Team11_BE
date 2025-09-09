package com.pnu.momeet.domain.profile.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.profile.dto.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.ProfileResponse;
import com.pnu.momeet.domain.profile.dto.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.service.ProfileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable UUID profileId) {
        return ResponseEntity.ok(profileService.getProfileById(profileId));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ProfileResponse> createMyProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ProfileCreateRequest request
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
        @Valid @RequestBody ProfileUpdateRequest request
    ) {
        ProfileResponse response = profileService.updateMyProfile(userDetails.getMemberId(), request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/image")
    public ResponseEntity<ProfileResponse> uploadProfileImage(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestPart("image") MultipartFile multipartFile
    ) {
        ProfileResponse response = profileService.updateProfileImageUrl(userDetails.getMemberId(), multipartFile);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/image")
    public ResponseEntity<Void> deleteProfileImage(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        profileService.deleteProfileImageUrl(userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
