package com.pnu.momeet.domain.meetup.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.meetup.service.MeetupStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/meetups")
@RequiredArgsConstructor
public class MeetupStateController {
    private final MeetupStateService meetupStateService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/me/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void meetupStart(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        meetupStateService.startMeetupByMemberId(userDetails.getMemberId());
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/me/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void meetupCancel(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        meetupStateService.cancelMeetup(userDetails.getMemberId());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("{meetupId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void meetupCancelAdmin(
        @PathVariable UUID meetupId
    ) {
        meetupStateService.cancelMeetupAdmin(meetupId);
    }

    @PostMapping("/me/finish")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void meetupFinish(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        meetupStateService.finishMeetupByMemberId(userDetails.getMemberId());
    }
}
