package com.pnu.momeet.domain.block.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.block.dto.response.BlockResponse;
import com.pnu.momeet.domain.block.service.BlockDomainService;
import com.pnu.momeet.domain.block.dto.request.BlockPageRequest;
import com.pnu.momeet.domain.profile.dto.response.BlockedProfileResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockDomainService blockService;

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @GetMapping({"/profiles"})
    public ResponseEntity<Page<BlockedProfileResponse>> getMyBlockedProfiles(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute BlockPageRequest pageRequest
    ) {
        return ResponseEntity.ok(
            blockService.getMyBlockedProfiles(userDetails.getMemberId(), pageRequest)
        );
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @PostMapping({"/{targetId}"})
    public ResponseEntity<BlockResponse> blockUser(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID targetId
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(blockService.createUserBlock(userDetails.getMemberId(), targetId));
    }

    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @DeleteMapping({"/{targetId}"})
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID targetId
    ) {
        blockService.deleteBlock(userDetails.getMemberId(), targetId);
        return ResponseEntity.noContent().build();
    }
}
