package com.pnu.momeet.domain.participant.repository;

import com.pnu.momeet.domain.participant.entity.Participant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    @EntityGraph("Participant.withProfile")
    List<Participant> findAllByMeetupId(UUID meetupId);

    @Query("""
        SELECT p FROM Participant p
        WHERE p.meetup.id = :meetupId
        ORDER BY p.profile.temperature DESC
        LIMIT 2
    """)
    List<Participant> findTopTwoByOrderByTemperatureDesc(UUID meetupId);

    Optional<Participant> findByProfileIdAndMeetupId(UUID profileId, UUID meetupId);

    boolean existsByMeetupIdAndProfileId(UUID meetup_id, UUID profile_id);

    boolean existsByIdAndMeetupId(Long id, UUID meetupId);

    @Query("""
        SELECT p FROM Participant p
        WHERE p.meetup.id = :meetupId AND p.profile.memberId = :memberId
    """)
    Optional<Participant> findByMemberIdAndMeetupId(UUID memberId, UUID meetupId);
}
