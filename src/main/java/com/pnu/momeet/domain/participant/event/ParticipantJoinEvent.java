package com.pnu.momeet.domain.participant.event;

import com.pnu.momeet.domain.participant.entity.Participant;

import java.util.UUID;

public class ParticipantJoinEvent extends ParticipantEvent {

    public ParticipantJoinEvent(
        UUID meetupId,
        Participant participant
    ) {
        super(meetupId, participant);
    }
}
