package com.pnu.momeet.domain.member.controller;

import com.pnu.momeet.domain.member.dto.request.*;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.service.mapper.MemberDtoMapper;
import com.pnu.momeet.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    //TODO: Member List 를 페이지네이션으로 반환하는 api 구현
    //TODO: Swagger을 이용한 API 문서화

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<MemberResponse>> memberList(
            @Valid @ModelAttribute MemberPageRequest request
    ) {
        var members = memberService.findAllMembers(MemberDtoMapper.toPageRequest(request));
        return ResponseEntity.ok(members);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> memberInfo(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(memberService.findMemberById(id));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> memberSelf(
            @AuthenticationPrincipal UserDetails userDetails
            ) {
        var member = memberService.findMemberById(UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(member);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<MemberResponse> memberCreate(
            @Valid @RequestBody MemberCreateRequest request
            ) {
        var savedMember = memberService.saveMember(MemberDtoMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMember);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<MemberResponse> memberEdit(
            @PathVariable UUID id,
            @Valid @RequestBody MemberEditRequest request
    ) {
        return ResponseEntity.ok(
                memberService.updateMemberById(
                        id, MemberDtoMapper.toConsumer(request)
                ));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/password")
    public ResponseEntity<MemberResponse> memberPasswordEdit(
            @PathVariable UUID id,
            @Valid @RequestBody AdminChangePasswordRequest request
    ) {
        var updatedMember = memberService.updatePasswordById(id, request.newPassword());
        return ResponseEntity.ok(updatedMember);
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/me/password")
    public ResponseEntity<MemberResponse> memberSelfPasswordEdit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        var updatedMember = memberService.validateAndUpdatePasswordById(
                UUID.fromString(userDetails.getUsername()),
                request.oldPassword(),
                request.newPassword()
        );
        return ResponseEntity.ok(updatedMember);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> memberDelete(
            @Valid @PathVariable UUID id
    ) {
        memberService.deleteMemberById(id);
        return ResponseEntity.noContent().build();
    }
}
