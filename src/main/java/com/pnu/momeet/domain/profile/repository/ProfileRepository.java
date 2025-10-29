package com.pnu.momeet.domain.profile.repository;

import com.pnu.momeet.domain.profile.entity.Profile;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID>, ProfileDslRepository {
    @EntityGraph(attributePaths = {"baseLocation"})
    Optional<Profile> findByMemberId(UUID memberId);

    boolean existsByMemberId(UUID memberId);

    boolean existsByNicknameIgnoreCase(String nickname);

    @Query("""
        SELECT p.id FROM Profile p WHERE p.memberId = :memberId
    """)
    Optional<UUID> findIdByMemberId(UUID memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profile p where p.id = :id")
    Optional<Profile> findByIdForUpdate(UUID id);
}
