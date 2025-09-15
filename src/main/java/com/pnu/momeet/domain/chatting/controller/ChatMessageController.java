package com.pnu.momeet.domain.chatting.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.chatting.dto.request.HistoryCursorRequest;
import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.service.ChatMessageService;
import com.pnu.momeet.domain.common.dto.response.CursorInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetups/{meetupId}/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping
    public ResponseEntity<CursorInfo<MessageResponse>> messageHistories(
            @ModelAttribute HistoryCursorRequest request,
            @PathVariable UUID meetupId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CursorInfo<MessageResponse> histories = chatMessageService.getHistories(
                meetupId, userDetails.getMemberId(), request.getSize(), request.getCursorId()
        );
        return ResponseEntity.ok(histories);
    }
}
