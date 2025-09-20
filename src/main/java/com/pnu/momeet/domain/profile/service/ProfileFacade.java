package com.pnu.momeet.domain.profile.service;

import com.pnu.momeet.domain.evaluation.service.EvaluationService;
import com.pnu.momeet.domain.meetup.dto.response.UnEvaluatedMeetupDto;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileFacade {

    private final MeetupService meetupService;
    private final EvaluationService evaluationService;
    private final ProfileRepository profileRepository;

    // 순환 참조를 방지하기 위해 ProfileFacade 클래스에 구현
    @Transactional(readOnly = true)
    public Page<UnEvaluatedMeetupDto> getUnEvaluatedMeetups(UUID memberId, Pageable pageable) {
        Profile me = profileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NoSuchElementException("프로필이 존재하지 않습니다."));

        Page<Meetup> meetups = meetupService.findEndedMeetupsByProfileId(me.getId(), pageable);

        return meetups.map(meetup -> {
            long unEvaluatedCount = evaluationService.calculateUnEvaluatedCount(meetup, me.getId());
            return MeetupEntityMapper.unEvaluatedMeetupDto(meetup, unEvaluatedCount);
        });
    }
}
