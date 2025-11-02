package com.pnu.momeet.domain.participant.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.service.ParticipantDomainService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetups/{meetupId}/participants")
@RequiredArgsConstructor
public class ParticipantController {
    private final ParticipantDomainService participantService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> participantsList(
            @PathVariable UUID meetupId
    ) {
        List<ParticipantResponse> participants = participantService.getParticipantsByMeetupId(meetupId);
        return ResponseEntity.ok().body(participants);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/visible")
    public ResponseEntity<List<ParticipantResponse>> getVisibleParticipants(
        @PathVariable UUID meetupId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ParticipantResponse> participants = participantService.getParticipantsVisibleToViewer(
            meetupId, userDetails.getMemberId()
        );
        return ResponseEntity.ok().body(participants);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<ParticipantResponse> getMyParticipantInfo(
            @PathVariable UUID meetupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ParticipantResponse response = participantService.getMyParticipantInfo(meetupId, userDetails.getMemberId());
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ParticipantResponse> joinMeetup(
            @PathVariable UUID meetupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ParticipantResponse response = participantService.joinMeetup(meetupId, userDetails.getMemberId());
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping
    public ResponseEntity<Void> leaveMeetup(
            @PathVariable UUID meetupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        participantService.leaveMeetup(meetupId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PatchMapping("/{participantId}/grant")
    public ResponseEntity<ParticipantResponse> grantRole(
            @PathVariable UUID meetupId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long participantId
    ) {
        ParticipantResponse response = participantService.
                grantHostRole(meetupId, userDetails.getMemberId(), participantId);
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{participantId}")
    public ResponseEntity<Void> kickParticipant(
            @PathVariable UUID meetupId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long participantId
    ) {
        participantService.kickParticipant(meetupId, userDetails.getMemberId(), participantId);
        return ResponseEntity.noContent().build();
    }
}
