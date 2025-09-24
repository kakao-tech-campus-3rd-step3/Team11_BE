package com.pnu.momeet.domain.participant.service;

import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantEntityService {
    private final ParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public List<Participant> getAllByMeetupId(UUID meetupId) {
        return participantRepository.findAllByMeetupId(meetupId);
    }

    @Transactional(readOnly = true)
    public Participant getById(Long id) {
        log.debug("특정 id의 참가자 조회 시도. id={}", id);
        var participant = participantRepository.findById(id);
        if (participant.isEmpty()) {
            log.warn("존재하지 않는 id의 참가자 조회 시도. id={}", id);
            throw new NoSuchElementException("참가자를 찾을 수 없습니다.");
        }
        log.debug("특정 id의 참가자 조회 성공. id={}", id);
        return participant.get();
    }

    @Transactional(readOnly = true)
    public Participant getByIdAndMeetupId(Long id, UUID meetupId) {
        log.debug("특정 id와 모임 ID의 참가자 조회 시도. id={}, meetupId={}", id, meetupId);
        var participant = participantRepository.findByIdAndMeetupId(id, meetupId);
        if (participant.isEmpty()) {
            log.warn("존재하지 않는 id와 모임 ID의 참가자 조회 시도. id={}, meetupId={}", id, meetupId);
            throw new NoSuchElementException("참가자를 찾을 수 없습니다.");
        }
        log.debug("특정 id와 모임 ID의 참가자 조회 성공. id={}, meetupId={}", id, meetupId);
        return participant.get();
    }

    @Transactional(readOnly = true)
    public List<Participant> getAllByProfileID(UUID profileId) {
        log.debug("프로필 ID로 참가자 조회 시도. profileId={}", profileId);
        var participants = participantRepository.findAllByProfileId(profileId);
        log.debug("프로필 ID로 참가자 조회 성공. profileId={}", profileId);
        return participants;
    }

    @Transactional(readOnly = true)
    public Participant getByProfileIDAndMeetupID(UUID profileId, UUID meetupId) {
        log.debug("프로필 ID와 모임 ID로 참가자 조회 시도. profileId={}, meetupId={}", profileId, meetupId);
        var participant = participantRepository.findByProfileIdAndMeetupId(profileId, meetupId);
        if (participant.isEmpty()) {
            log.warn("존재하지 않는 프로필 ID와 모임 ID로 참가자 조회 시도. profileId={}, meetupId={}", profileId, meetupId);
            throw new NoSuchElementException("참가자를 찾을 수 없습니다.");
        }
        log.debug("프로필 ID와 모임 ID로 참가자 조회 성공. profileId={}, meetupId={}", profileId, meetupId);
        return participant.get();
    }

    @Transactional(readOnly = true)
    public boolean existsByIdAndMeetupId(Long id, UUID meetupId) {
        return participantRepository.existsByIdAndMeetupId(id, meetupId);
    }

    @Transactional(readOnly = true)
    public boolean existsByProfileIdAndMeetupId(UUID profileId, UUID meetupId) {
        return participantRepository.existsByProfileIdAndMeetupId(profileId, meetupId);
    }


    @Transactional
    public Pair<Participant, Participant> getTopTwoByTemperatureDesc(UUID meetupId) {
        log.debug("모임 ID로 상위 2명의 참가자 조회 시도. meetupId={}", meetupId);
        var participants = participantRepository.findTopTwoByOrderByTemperatureDesc(meetupId);
        if (participants.size() < 2) {
            log.warn("상위 2명의 참가자가 존재하지 않음. meetupId={}", meetupId);
            throw new NoSuchElementException("상위 2명의 참가자가 존재하지 않습니다.");
        }
        log.debug("모임 ID로 상위 2명의 참가자 조회 성공. meetupId={}", meetupId);
        return Pair.of(participants.get(0), participants.get(1));
    }

    @Transactional
    public Participant createParticipant(Participant participant) {
        log.debug("모임 참가자 생성 시도. meetupId={}, profileId={}", participant.getMeetup().getId(), participant.getProfile().getId());
        var createdParticipant = participantRepository.save(participant);
        log.debug("모임 참가자 생성 성공. id={}, meetupId={}", createdParticipant.getId(), createdParticipant.getMeetup().getId());
        return createdParticipant;
    }

    @Transactional
    public Participant updateParticipant(Participant participant, Consumer<Participant> updater) {
        log.debug("특정 id의 참가자 정보 수정 시도. id={}", participant.getId());
        updater.accept(participant);
        log.debug("특정 id의 참가자 정보 수정 성공. id={}", participant.getId());
        return participant;
    }

    @Transactional
    public Participant saveParticipant(Participant participant) {
        return participantRepository.save(participant);
    }

    @Transactional
    public void deleteById(Long id) {
        log.debug("특정 id의 참가자 삭제 시도. id={}", id);
        if (!participantRepository.existsById(id)) {
            log.warn("존재하지 않는 id의 참가자 삭제 시도. id={}", id);
            throw new NoSuchElementException("참가자를 찾을 수 없습니다.");
        }
        participantRepository.deleteById(id);
        log.debug("특정 id의 참가자 삭제 성공. id={}", id);
    }


}
