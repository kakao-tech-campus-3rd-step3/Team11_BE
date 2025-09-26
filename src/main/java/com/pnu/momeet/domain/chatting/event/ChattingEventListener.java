package com.pnu.momeet.domain.chatting.event;

import com.pnu.momeet.domain.chatting.service.ChattingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChattingEventListener {

    private static final String MEETUP_TOPIC_PREFIX = "/topic/meetups/";
    private final ChattingService chattingService;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.debug("WebSocket 세션 연결됨 - sessionId: {}", sessionId);
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        if (destination != null) {
            if (destination.startsWith(MEETUP_TOPIC_PREFIX)) {
                log.debug("WebSocket 구독 해제 - destination: {}", destination);

                UUID meetupId = getMeetupIdFromDestination(destination);
                UUID memberId = getMemberIdFromPrincipal(accessor.getUser());
                if (meetupId == null || memberId == null) {
                    return;
                }
                chattingService.disconnectFromMeetup(meetupId, memberId);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.debug("WebSocket 세션 연결 해제됨 - sessionId: {}", sessionId);
        UUID memberId = getMemberIdFromPrincipal(accessor.getUser());
        if (memberId != null) {
            chattingService.disconnectAllFromMeetup(memberId);
        }
    }

    private UUID getMeetupIdFromDestination(String destination) {
      try {
          String[] parts = destination.split("/");
          return UUID.fromString(parts[3]);
      } catch (Exception e) {
          return null;
      }
    }

    private UUID getMemberIdFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }
        return UUID.fromString(principal.getName());
    }
}
