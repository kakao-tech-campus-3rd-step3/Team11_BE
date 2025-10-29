package com.pnu.momeet.domain.profile.repository;

import com.pnu.momeet.domain.profile.dto.response.BlockedProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProfileDslRepository {
    Page<BlockedProfileResponse> findBlockedProfiles(UUID blockerId, Pageable pageable);
}
