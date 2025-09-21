package com.pnu.momeet.domain.badge.service;

import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.repository.BadgeDslRepository;
import com.pnu.momeet.domain.badge.service.mapper.BadgeDtoMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeDslRepository badgeDslRepository;
    private final ProfileService profileService;

    @Transactional(readOnly = true)
    public Page<BadgeResponse> getMyBadges(UUID memberId, BadgePageRequest badgePageRequest) {
        PageRequest pageRequest = BadgeDtoMapper.toPageRequest(badgePageRequest);
        Profile profile = profileService.getProfileEntityByMemberId(memberId);
        return badgeDslRepository.findMyBadges(profile.getId(), pageRequest);
    }
}
