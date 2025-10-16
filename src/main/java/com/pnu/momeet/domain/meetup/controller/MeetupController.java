package com.pnu.momeet.domain.meetup.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupGeoSearchRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupPageRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupUpdateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/meetups")
@RequiredArgsConstructor
public class MeetupController {

    private final MeetupDomainService meetupService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping
    public Page<MeetupResponse> meetupPage(
        @Valid  @ModelAttribute MeetupPageRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return meetupService.getAllBySpecification(request, userDetails.getMemberId());
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/geo")
    public ResponseEntity<List<MeetupResponse>> meetupGeoSearch(
        @Valid @ModelAttribute MeetupGeoSearchRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(meetupService.getAllByLocation(request, userDetails.getMemberId()));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{meetupId}")
    public ResponseEntity<MeetupDetail> meetupResponse(
        @PathVariable UUID meetupId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(meetupService.getById(meetupId, userDetails.getMemberId()));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<MeetupDetail> meetupSelf(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(meetupService.getOwnedActiveMeetupByMemberID(userDetails.getMemberId()));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<MeetupDetail> createMeetup(
            @Valid @RequestBody  MeetupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        var createdMeetup = meetupService.createMeetup(request, userDetails.getMemberId());
        return ResponseEntity.created(URI.create("/api/meetups/" + createdMeetup.id()))
                .body(createdMeetup);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/me")
    public ResponseEntity<MeetupDetail> updateMeetup(
            @Valid @RequestBody  MeetupUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                meetupService.updateMeetupByMemberId(request, userDetails.getMemberId())
        );
    }
}
