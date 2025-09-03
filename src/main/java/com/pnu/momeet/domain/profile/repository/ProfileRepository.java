package com.pnu.momeet.domain.profile.repository;

import com.pnu.momeet.domain.profile.entity.Profile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByMemberId(UUID memberId);

    boolean existsByMemberId(UUID memberId);

    boolean existsByNicknameIgnoreCase(String nickname);
}
