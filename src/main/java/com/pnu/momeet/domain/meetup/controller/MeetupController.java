package com.pnu.momeet.domain.meetup.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.MeetupDetailResponse;
import com.pnu.momeet.domain.meetup.dto.MeetupResponse;
import com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetups")
@RequiredArgsConstructor
public class MeetupController {

    private final MeetupService meetupService;

    @GetMapping
    public ResponseEntity<List<MeetupResponse>> getAllMeetups(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        List<MeetupResponse> response = meetupService.getAllMeetups(category, status, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{meetupId}")
    public ResponseEntity<MeetupDetailResponse> getMeetupById(@PathVariable UUID meetupId) {
        MeetupDetailResponse response = meetupService.getMeetupById(meetupId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<MeetupResponse> createMeetup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MeetupCreateRequest request
    ) {
        MeetupResponse response = meetupService.createMeetup(userDetails.getMemberId(), request);
        return ResponseEntity
                .created(URI.create("/api/meetups/" + response.getId()))
                .body(response);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/{meetupId}")
    public ResponseEntity<MeetupResponse> updateMeetup(
            @PathVariable UUID meetupId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MeetupUpdateRequest request
    ) {
        MeetupResponse response = meetupService.updateMeetup(meetupId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{meetupId}")
    public ResponseEntity<Void> deleteMeetup(
            @PathVariable UUID meetupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        meetupService.deleteMeetup(meetupId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
