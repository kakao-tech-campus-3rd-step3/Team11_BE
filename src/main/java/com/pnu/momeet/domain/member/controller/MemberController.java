package com.pnu.momeet.domain.member.controller;

import com.pnu.momeet.domain.member.dto.*;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.mapper.DtoMapper;
import com.pnu.momeet.domain.member.mapper.EntityMapper;
import com.pnu.momeet.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<MemberResponse>> memberList() {
        List<Member> members = memberService.findAllMembers();
        return ResponseEntity.ok(members.stream()
                .map(EntityMapper::toDto)
                .toList()
        );
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> memberInfo(
            @PathVariable UUID id
    ) {
        Member member = memberService.findMemberById(id);
        return ResponseEntity.ok(EntityMapper.toDto(member));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> memberSelf(
            @AuthenticationPrincipal UserDetails userDetails
            ) {
        Member member = memberService.findMemberById(UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(EntityMapper.toDto(member));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<MemberResponse> memberCreate(
            @Valid @RequestBody MemberCreateRequest request
            ) {
        Member savedMember = memberService.saveMember(DtoMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EntityMapper.toDto(savedMember));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<MemberResponse> memberEdit(
            @PathVariable UUID id,
            @Valid @RequestBody MemberEditRequest request
    ) {
        Member updatedMember = memberService.updateMemberById(id, DtoMapper.toConsumer(request));
        return ResponseEntity.ok(EntityMapper.toDto(updatedMember));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/password")
    public ResponseEntity<MemberResponse> memberPasswordEdit(
            @PathVariable UUID id,
            @Valid @RequestBody AdminChangePasswordRequest request
    ) {
        Member updatedMember = memberService.updatePasswordById(id, request.newPassword());
        return ResponseEntity.ok(EntityMapper.toDto(updatedMember));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/me/password")
    public ResponseEntity<MemberResponse> memberSelfPasswordEdit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Member updatedMember = memberService.validateAndUpdatePasswordById(
                UUID.fromString(userDetails.getUsername()),
                request.oldPassword(),
                request.newPassword()
        );
        return ResponseEntity.ok(EntityMapper.toDto(updatedMember));
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
