package com.pnu.momeet.domain.participant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BanParticipantService {
    private final static String PREFIX_TEMPLATE = "MEETUP::%s::BANNED_PROFILE";
    private final RedisTemplate<String, String> redisTemplate;

    public String getKey(UUID meetupId) {
        return String.format(PREFIX_TEMPLATE, meetupId.toString());
    }

    public void banProfileId(UUID meetupId, UUID profileId) {
        redisTemplate.opsForSet().add(
                getKey(meetupId),
                profileId.toString()
        );
    }

    public void clearBanList(UUID meetupId) {
        redisTemplate.delete(getKey(meetupId));
    }

    public boolean isBanned(UUID meetupId, UUID profileId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(
                getKey(meetupId), profileId.toString()
        ));
    }
}
