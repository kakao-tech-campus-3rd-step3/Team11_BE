package com.pnu.momeet.domain.meetup.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.MeetupResponse;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
}
