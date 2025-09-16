package com.pnu.momeet.domain.participant.repository;

import com.pnu.momeet.domain.meetup.entity.QMeetup;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.entity.QParticipant;
import com.pnu.momeet.domain.profile.entity.QProfile;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ParticipantDslRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public List<Participant> findJoiningParticipantsByMemberId(UUID memberId) {
        QParticipant participant = QParticipant.participant;
        QProfile profile = QProfile.profile;
        QMeetup meetup = QMeetup.meetup;

        UUID profileId = jpaQueryFactory
                .select(profile.id)
                .from(profile)
                .where(profile.memberId.eq(memberId))
                .fetchOne();

        if (profileId == null) {
            return List.of();
        }

        return jpaQueryFactory
            .selectFrom(participant)
            .join(participant.meetup, meetup)
            .where(
                participant.profile.id.eq(profileId),
                participant.meetup.status.in(MeetupStatus.OPEN, MeetupStatus.IN_PROGRESS)
            )
        .fetch();
    }
}
