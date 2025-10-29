package com.pnu.momeet.domain.participant.service.listener;

import com.pnu.momeet.domain.chatting.enums.ChatActionType;
import com.pnu.momeet.domain.chatting.util.ChatMessagingTemplate;
import com.pnu.momeet.domain.participant.event.ParticipantExitEvent;
import com.pnu.momeet.domain.participant.event.ParticipantJoinEvent;
import com.pnu.momeet.domain.participant.event.ParticipantKickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantActionHandlingListener {
    private final ChatMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleParticipantJoinEvent(ParticipantJoinEvent event) {
        messagingTemplate.sendAction(
                event.getMeetupId(),
                event.getParticipant(),
                ChatActionType.JOIN
        );
        log.debug("참여자 참가 알림 전송 완료 - meetupId: {}, participantId: {}",
                event.getMeetupId(),
                event.getParticipant().getId()
        );
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleParticipantKickEvent(ParticipantKickEvent event) {
        messagingTemplate.sendAction(
                event.getMeetupId(),
                event.getParticipant(),
                ChatActionType.KICKED
        );

        log.debug("참여자 강제 퇴장 알림 전송 완료 - meetupId: {}, participantId: {}",
                event.getMeetupId(),
                event.getParticipant().getId()
        );
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleParticipantLeaveEvent(ParticipantExitEvent event) {
        messagingTemplate.sendAction(
                event.getMeetupId(),
                event.getParticipant(),
                ChatActionType.EXIT
        );
        log.debug("참여자 퇴장 알림 전송 완료 - meetupId: {}, participantId: {}",
                event.getMeetupId(),
                event.getParticipant().getId()
        );
    }
}
