package com.pnu.momeet.domain.meetup.controller;

import com.pnu.momeet.domain.meetup.dto.MeetupResponse;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
