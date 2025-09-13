package com.pnu.momeet.domain.meetup.controller;

import com.pnu.momeet.common.exception.CustomValidationException;
import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupGeoSearchRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupPageRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupUpdateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetups")
@RequiredArgsConstructor
public class MeetupController {

    private final MeetupService meetupService;

    private void validateCategories(String mainCategory, String subCategory) {
        if (mainCategory != null && subCategory != null) {
            if (!SubCategory.valueOf(subCategory).getMainCategory().name().equals(mainCategory)) {
                throw new CustomValidationException(Map.of(
                        "subCategory", List.of("서브 카테고리가 메인 카테고리에 속하지 않습니다.")
                ));
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping
    public Page<MeetupResponse> meetupPage(
            @Valid  @ModelAttribute MeetupPageRequest request
    ) {
        return meetupService.findAllBySpecification(request);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/geo")
    public ResponseEntity<List<MeetupResponse>> meetupGeoSearch(
            @Valid @ModelAttribute MeetupGeoSearchRequest request
            ) {
        return ResponseEntity.ok(meetupService.findAllByLocation(request));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{meetupId}")
    public ResponseEntity<MeetupResponse> meetupResponse(
            @PathVariable UUID meetupId
    ) {
        return ResponseEntity.ok(meetupService.findById(meetupId));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<MeetupResponse> meetupSelf(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(meetupService.findByMemberId(userDetails.getMemberId()));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<MeetupResponse> createMeetup(
            @Valid @RequestBody  MeetupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        validateCategories(request.category(), request.subCategory());
        var createdMeetup = meetupService.createMeetup(request, userDetails.getMemberId());
        return ResponseEntity.created(URI.create("/api/meetups/" + createdMeetup.id()))
                .body(createdMeetup);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/me")
    public ResponseEntity<MeetupResponse> updateMeetup(
            @Valid @RequestBody  MeetupUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        validateCategories(request.category(), request.subCategory());
        return ResponseEntity.ok(
                meetupService.updateMeetupByMemberId(request, userDetails.getMemberId())
        );
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMeetup(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        meetupService.deleteMeetup(userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{meetupId}")
    public ResponseEntity<Void> adminDeleteMeetup(
            @PathVariable UUID meetupId
    ) {
        meetupService.deleteMeetupAdmin(meetupId);
        return ResponseEntity.noContent().build();
    }
}
