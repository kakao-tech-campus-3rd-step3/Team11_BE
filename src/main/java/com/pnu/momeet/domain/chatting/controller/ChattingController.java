package com.pnu.momeet.domain.chatting.controller;

import com.pnu.momeet.common.security.details.CustomUserDetails;
import com.pnu.momeet.domain.chatting.dto.request.MessageRequest;
import com.pnu.momeet.domain.chatting.service.ChattingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChattingController {

    private final ChattingService chatMessageService;

    @MessageMapping("/meetups/{meetupId}/messages")
    public void sendMessage(
            @DestinationVariable UUID meetupId,
            MessageRequest messageRequest,
            @AuthenticationPrincipal Principal principal
            ) {
        UUID memberId = UUID.fromString(principal.getName());
        chatMessageService.sendMessage(
            meetupId, memberId, messageRequest
        );
    }

    @SubscribeMapping("/meetups/{meetupId}/actions")
    public void subscribeActions(
            @DestinationVariable UUID meetupId,
            @AuthenticationPrincipal Principal principal
    ) {
        UUID memberId = UUID.fromString(principal.getName());
        chatMessageService.connectToMeetup(meetupId, memberId);
    }

    @SubscribeMapping("/meetups/{meetupId}/messages")
    public void subscribeMessages(
            @DestinationVariable UUID meetupId,
            @AuthenticationPrincipal Principal principal
    ) {
        UUID memberId = UUID.fromString(principal.getName());
        chatMessageService.connectToMeetup(meetupId, memberId);
    }
}
