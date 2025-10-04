package com.pnu.momeet.domain.meetup.repository;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MeetupRepository extends JpaRepository<Meetup, UUID>, JpaSpecificationExecutor<Meetup> {
    @EntityGraph(attributePaths = {"hashTags"})
    @NonNull
    Page<Meetup> findAll(Specification<Meetup> spec,@NonNull Pageable pageable);
    List<Meetup> findAllByStatusAndEndAtBefore(MeetupStatus status, LocalDateTime time);
}