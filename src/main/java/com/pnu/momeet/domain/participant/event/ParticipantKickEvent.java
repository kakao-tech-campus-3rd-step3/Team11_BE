package com.pnu.momeet.domain.participant.event;

import java.util.UUID;

public class ParticipantKickEvent extends ParticipantEvent {

    public ParticipantKickEvent(
        UUID meetupId,
        Long participantId
    ) {
        super(meetupId, participantId);
    }
}
