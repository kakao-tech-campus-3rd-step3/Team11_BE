package com.pnu.momeet.domain.profile.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.meetup.dto.response.UnEvaluatedMeetupDto;
import com.pnu.momeet.domain.profile.dto.request.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.request.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.service.ProfileFacade;
import com.pnu.momeet.domain.profile.service.ProfileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    private final ProfileService profileService;
    private final ProfileFacade profileFacade;

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
    @GetMapping("/me/unEvaluated-meetups")
    public ResponseEntity<Page<UnEvaluatedMeetupDto>> getUnEvaluatedMeetups(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<UnEvaluatedMeetupDto> result =
            profileFacade.getUnEvaluatedMeetups(userDetails.getMemberId(), pageable);

        return ResponseEntity.ok(result);
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
