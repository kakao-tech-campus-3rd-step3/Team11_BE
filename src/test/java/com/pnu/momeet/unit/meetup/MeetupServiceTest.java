package com.pnu.momeet.unit.meetup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pnu.momeet.domain.meetup.dto.MeetupResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MeetupCategory;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.repository.MemberRepository;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.*;

@Tag("meetup")
@Tag("service")
class MeetupServiceTest {

    private MockMeetupRepository mockMeetupRepository;
    private MockMemberRepository mockMemberRepository;
    private MockProfileRepository mockProfileRepository;
    private GeometryFactory geometryFactory;
    private MeetupService meetupService;

    @BeforeEach
    void setUp() {
        mockMeetupRepository = new MockMeetupRepository();
        mockMemberRepository = new MockMemberRepository();
        mockProfileRepository = new MockProfileRepository();
        geometryFactory = new GeometryFactory();
        meetupService = new MeetupService(mockMeetupRepository, mockProfileRepository, mockMemberRepository);
    }

    @Test
    @DisplayName("전체 모임 조회 성공 - 필터링 없이")
    void getAllMeetups_success_noFilter() {
        // given
        Member owner = new Member("test@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Point point = geometryFactory.createPoint(new Coordinate(129.0897, 35.2431));
        
        Meetup meetup = new Meetup(
            owner, "테스트 모임", MeetupCategory.SPORTS, "모임 설명",
            new String[]{"태그1"}, new String[]{"해시태그1"}, 10, 70,
            point, "부산시 금정구", 
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        meetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(meetup);

        Profile profile = Profile.create(owner.getId(), "테스트유저", 25, Gender.MALE, 
            "image.jpg", "소개글", "부산시");
        profile.setId(UUID.randomUUID());
        mockProfileRepository.save(profile);

        // when
        List<MeetupResponse> result = meetupService.getAllMeetups(null, null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("테스트 모임");
        assertThat(result.getFirst().getCategory()).isEqualTo(MeetupCategory.SPORTS);
        assertThat(result.getFirst().getOwnerProfile().getNickname()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("모임 생성 성공")
    void createMeetup_success() {
        // given
        Member owner = new Member("test@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Profile profile = Profile.create(owner.getId(), "생성자", 30, Gender.FEMALE, 
            "profile.jpg", "자기소개", "부산시");
        profile.setId(UUID.randomUUID());
        mockProfileRepository.save(profile);

        var locationRequest = new com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest.LocationRequest(
            35.2431, 129.0897, "부산시 금정구"
        );

        var createRequest = new com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest(
            "새로운 모임", MeetupCategory.STUDY, "스터디 모임입니다",
            new String[]{"스터디", "개발"}, new String[]{"#Java", "#Spring"},
            5, 80,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), locationRequest
        );

        // when
        MeetupResponse result = meetupService.createMeetup(owner.getId(), createRequest);

        // then
        assertThat(result.getName()).isEqualTo("새로운 모임");
        assertThat(result.getCategory()).isEqualTo(MeetupCategory.STUDY);
        assertThat(result.getDescription()).isEqualTo("스터디 모임입니다");
        assertThat(result.getCapacity()).isEqualTo(5);
        assertThat(result.getScoreLimit()).isEqualTo(80);
        assertThat(result.getOwnerProfile().getNickname()).isEqualTo("생성자");
        assertThat(result.getLocation().getLatitude()).isEqualTo(35.2431);
        assertThat(result.getLocation().getLongitude()).isEqualTo(129.0897);
    }

    @Test
    @DisplayName("모임 생성 실패 - 존재하지 않는 회원")
    void createMeetup_fail_memberNotFound() {
        // given
        UUID nonExistentMemberId = UUID.randomUUID();

        var locationRequest = new com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest.LocationRequest(
            35.2431, 129.0897, "부산시 금정구"
        );

        var createRequest = new com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest(
            "테스트 모임", MeetupCategory.SPORTS, "모임 설명",
            new String[]{"태그"}, new String[]{"해시태그"},
            10, 70,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), locationRequest
        );

        // when & then
        assertThatThrownBy(() -> meetupService.createMeetup(nonExistentMemberId, createRequest))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("모임 생성 실패 - 시작시간이 종료시간보다 늦음")
    void createMeetup_fail_invalidTimeRange() {
        // given
        Member owner = new Member("test@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        var locationRequest = new com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest.LocationRequest(
            35.2431, 129.0897, "부산시 금정구"
        );

        var createRequest = new com.pnu.momeet.domain.meetup.dto.MeetupCreateRequest(
            "테스트 모임", MeetupCategory.SPORTS, "모임 설명",
            new String[]{"태그"}, new String[]{"해시태그"},
            10, 70,
            LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1), locationRequest // 시작시간이 종료시간보다 늦음
        );

        // when & then
        assertThatThrownBy(() -> meetupService.createMeetup(owner.getId(), createRequest))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("모임 단일 조회 성공")
    void getMeetupById_success() {
        // given
        Member owner = new Member("owner@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Profile profile = Profile.create(owner.getId(), "모임장", 28, Gender.MALE, 
            "owner.jpg", "모임장 소개", "부산시");
        profile.setId(UUID.randomUUID());
        mockProfileRepository.save(profile);

        Point point = geometryFactory.createPoint(new Coordinate(129.0756, 35.1796));
        Meetup meetup = new Meetup(
            owner, "단일 조회 테스트", MeetupCategory.HOBBY, "취미 모임",
            new String[]{"취미"}, new String[]{"#취미"},
            8, 60, point, "부산시 해운대구",
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        meetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(meetup);

        // when
        var result = meetupService.getMeetupById(meetup.getId());

        // then
        assertThat(result.getId()).isEqualTo(meetup.getId());
        assertThat(result.getName()).isEqualTo("단일 조회 테스트");
        assertThat(result.getCategory()).isEqualTo(MeetupCategory.HOBBY);
        assertThat(result.getOwnerProfile().getNickname()).isEqualTo("모임장");
        assertThat(result.getParticipants()).isEmpty(); // TODO로 빈 리스트
    }

    @Test
    @DisplayName("모임 단일 조회 실패 - 존재하지 않는 모임")
    void getMeetupById_fail_meetupNotFound() {
        // given
        UUID nonExistentMeetupId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> meetupService.getMeetupById(nonExistentMeetupId))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("모임 업데이트 성공")
    void updateMeetup_success() {
        // given
        Member owner = new Member("owner@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Profile profile = Profile.create(owner.getId(), "업데이트테스트", 32, Gender.MALE, 
            "update.jpg", "업데이트 테스터", "대구시");
        profile.setId(UUID.randomUUID());
        mockProfileRepository.save(profile);

        Point originalPoint = geometryFactory.createPoint(new Coordinate(128.6014, 35.8714));
        Meetup meetup = new Meetup(
            owner, "원본 모임", MeetupCategory.SPORTS, "원본 설명",
            new String[]{"원본태그"}, new String[]{"#원본"},
            10, 70, originalPoint, "대구시 중구",
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        meetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(meetup);

        var locationRequest = new com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest.LocationRequest(
            35.8714, 128.6014, "대구시 동구"
        );

        var updateRequest = new com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest(
            "업데이트된 모임", MeetupCategory.STUDY, "업데이트된 설명",
            new String[]{"업데이트태그"}, new String[]{"#업데이트"},
            15, 80,
            com.pnu.momeet.domain.meetup.enums.MeetupStatus.OPEN,
            LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4), locationRequest
        );

        // when
        MeetupResponse result = meetupService.updateMeetup(meetup.getId(), owner.getId(), updateRequest);

        // then
        assertThat(result.getName()).isEqualTo("업데이트된 모임");
        assertThat(result.getCategory()).isEqualTo(MeetupCategory.STUDY);
        assertThat(result.getDescription()).isEqualTo("업데이트된 설명");
        assertThat(result.getCapacity()).isEqualTo(15);
        assertThat(result.getScoreLimit()).isEqualTo(80);
        assertThat(result.getLocation().getAddress()).isEqualTo("대구시 동구");
    }

    @Test
    @DisplayName("모임 업데이트 실패 - 존재하지 않는 모임")
    void updateMeetup_fail_meetupNotFound() {
        // given
        UUID nonExistentMeetupId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        var locationRequest = new com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest.LocationRequest(
            35.2431, 129.0897, "부산시 금정구"
        );

        var updateRequest = new com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest(
            "업데이트 모임", MeetupCategory.STUDY, "업데이트 설명",
            new String[]{"태그"}, new String[]{"해시태그"},
            10, 70,
            com.pnu.momeet.domain.meetup.enums.MeetupStatus.OPEN,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), locationRequest
        );

        // when & then
        assertThatThrownBy(() -> meetupService.updateMeetup(nonExistentMeetupId, ownerId, updateRequest))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("모임 업데이트 실패 - 권한 없음")
    void updateMeetup_fail_noPermission() {
        // given
        Member owner = new Member("owner@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Member otherMember = new Member("other@test.com", "password", List.of(Role.ROLE_USER));
        otherMember.setId(UUID.randomUUID());
        mockMemberRepository.save(otherMember);

        Point point = geometryFactory.createPoint(new Coordinate(129.0897, 35.2431));
        Meetup meetup = new Meetup(
            owner, "권한 테스트", MeetupCategory.SPORTS, "권한 테스트 모임",
            new String[]{"테스트"}, new String[]{"#테스트"},
            10, 70, point, "부산시 금정구",
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        meetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(meetup);

        var locationRequest = new com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest.LocationRequest(
            35.2431, 129.0897, "부산시 금정구"
        );

        var updateRequest = new com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest(
            "무단 업데이트", MeetupCategory.STUDY, "무단 업데이트 시도",
            new String[]{"무단"}, new String[]{"#무단"},
            5, 80,
            com.pnu.momeet.domain.meetup.enums.MeetupStatus.OPEN,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), locationRequest
        );

        // when & then
        assertThatThrownBy(() -> meetupService.updateMeetup(meetup.getId(), otherMember.getId(), updateRequest))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("모임 업데이트 실패 - 시작시간이 종료시간보다 늦음")
    void updateMeetup_fail_invalidTimeRange() {
        // given
        Member owner = new Member("owner@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Point point = geometryFactory.createPoint(new Coordinate(129.0897, 35.2431));
        Meetup meetup = new Meetup(
            owner, "시간 테스트", MeetupCategory.SPORTS, "시간 테스트 모임",
            new String[]{"테스트"}, new String[]{"#테스트"},
            10, 70, point, "부산시 금정구",
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        meetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(meetup);

        var locationRequest = new com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest.LocationRequest(
            35.2431, 129.0897, "부산시 금정구"
        );

        var updateRequest = new com.pnu.momeet.domain.meetup.dto.MeetupUpdateRequest(
            "시간 오류 테스트", MeetupCategory.STUDY, "시간 오류",
            new String[]{"오류"}, new String[]{"#오류"},
            10, 70,
            com.pnu.momeet.domain.meetup.enums.MeetupStatus.OPEN,
            LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1), locationRequest // 시작 > 종료
        );

        // when & then
        assertThatThrownBy(() -> meetupService.updateMeetup(meetup.getId(), owner.getId(), updateRequest))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("모임 삭제 성공")
    void deleteMeetup_success() {
        // given
        Member owner = new Member("owner@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Point point = geometryFactory.createPoint(new Coordinate(129.0897, 35.2431));
        Meetup meetup = new Meetup(
            owner, "삭제할 모임", MeetupCategory.STUDY, "삭제 테스트 모임",
            new String[]{"삭제"}, new String[]{"#삭제"},
            10, 70, point, "부산시 금정구",
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        meetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(meetup);

        // 삭제 전에 모임이 존재하는지 확인
        assertThat(mockMeetupRepository.findById(meetup.getId())).isPresent();

        // when
        meetupService.deleteMeetup(meetup.getId(), owner.getId());

        // then
        assertThat(mockMeetupRepository.findById(meetup.getId())).isEmpty();
    }

    @Test
    @DisplayName("모임 삭제 실패 - 존재하지 않는 모임")
    void deleteMeetup_fail_meetupNotFound() {
        // given
        UUID nonExistentMeetupId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> meetupService.deleteMeetup(nonExistentMeetupId, ownerId))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("모임 삭제 실패 - 권한 없음")
    void deleteMeetup_fail_noPermission() {
        // given
        Member owner = new Member("owner@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Member otherMember = new Member("other@test.com", "password", List.of(Role.ROLE_USER));
        otherMember.setId(UUID.randomUUID());
        mockMemberRepository.save(otherMember);

        Point point = geometryFactory.createPoint(new Coordinate(129.0897, 35.2431));
        Meetup meetup = new Meetup(
            owner, "권한 테스트 모임", MeetupCategory.HOBBY, "권한 테스트",
            new String[]{"권한"}, new String[]{"#권한"},
            10, 70, point, "부산시 금정구",
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        meetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(meetup);

        // when & then
        assertThatThrownBy(() -> meetupService.deleteMeetup(meetup.getId(), otherMember.getId()))
            .isInstanceOf(IllegalStateException.class);

        // 삭제되지 않았는지 확인
        assertThat(mockMeetupRepository.findById(meetup.getId())).isPresent();
    }

    @Test
    @DisplayName("모임 조회 - 카테고리 필터링")
    void getAllMeetups_withCategoryFilter() {
        // given
        Member owner = new Member("test@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Profile profile = Profile.create(owner.getId(), "테스터", 25, Gender.MALE, 
            "test.jpg", "소개", "부산시");
        profile.setId(UUID.randomUUID());
        mockProfileRepository.save(profile);

        Point point = geometryFactory.createPoint(new Coordinate(129.0897, 35.2431));

        // 스포츠 모임
        Meetup sportsMeetup = new Meetup(
            owner, "축구 모임", MeetupCategory.SPORTS, "축구하자",
            new String[]{"축구"}, new String[]{"#축구"}, 10, 70,
            point, "부산시 금정구", 
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        sportsMeetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(sportsMeetup);

        // 스터디 모임
        Meetup studyMeetup = new Meetup(
            owner, "자바 스터디", MeetupCategory.STUDY, "자바 공부하자",
            new String[]{"자바"}, new String[]{"#자바"}, 5, 80,
            point, "부산시 금정구", 
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        studyMeetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(studyMeetup);

        // when
        List<MeetupResponse> result = meetupService.getAllMeetups("SPORTS", null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("축구 모임");
        assertThat(result.getFirst().getCategory()).isEqualTo(MeetupCategory.SPORTS);
    }

    @Test
    @DisplayName("모임 조회 - 검색어 필터링")
    void getAllMeetups_withSearchFilter() {
        // given
        Member owner = new Member("test@test.com", "password", List.of(Role.ROLE_USER));
        owner.setId(UUID.randomUUID());
        mockMemberRepository.save(owner);

        Profile profile = Profile.create(owner.getId(), "검색테스터", 30, Gender.FEMALE, 
            "search.jpg", "검색 테스트", "부산시");
        profile.setId(UUID.randomUUID());
        mockProfileRepository.save(profile);

        Point point = geometryFactory.createPoint(new Coordinate(129.0756, 35.1796));

        // 자바 관련 모임
        Meetup javaMeetup = new Meetup(
            owner, "Spring Boot 스터디", MeetupCategory.STUDY, "Spring Boot로 웹 개발을 배우자",
            new String[]{"Spring", "Java"}, new String[]{"#SpringBoot", "#Java"}, 8, 75,
            point, "부산시", 
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        javaMeetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(javaMeetup);

        // React 관련 모임
        Meetup reactMeetup = new Meetup(
            owner, "React 스터디", MeetupCategory.STUDY, "React로 프론트엔드 개발하기",
            new String[]{"React", "Frontend"}, new String[]{"#React", "#Frontend"}, 6, 70,
            point, "부산시", 
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        reactMeetup.setId(UUID.randomUUID());
        mockMeetupRepository.save(reactMeetup);

        // when
        List<MeetupResponse> result = meetupService.getAllMeetups(null, null, "Spring");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Spring Boot 스터디");
        assertThat(result.getFirst().getDescription()).contains("Spring Boot");
    }

    // 간단한 Mock Repository 구현 - 필요한 메서드만 구현
    // Meetup Mock
    static class MockMeetupRepository implements MeetupRepository {
        private final Map<UUID, Meetup> storage = new HashMap<>();

        @Override
        public <S extends Meetup> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public List<Meetup> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public Optional<Meetup> findById(UUID id) {
            return Optional.ofNullable(storage.get(id));
        }

        // 나머지 메서드들은 일단 UnsupportedOperationException으로 처리
        @Override
        public boolean existsById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public boolean existsByIdAndOwner_Id(UUID id, UUID ownerId) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Optional<Meetup> findByIdAndOwner_Id(UUID meetupId, UUID ownerId) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findByStatus(com.pnu.momeet.domain.meetup.enums.MeetupStatus status, org.springframework.data.domain.Pageable pageable) {
            List<Meetup> filtered = storage.values().stream()
                .filter(meetup -> meetup.getStatus().equals(status))
                .toList();
            return new org.springframework.data.domain.PageImpl<>(filtered);
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findByCategory(MeetupCategory category, org.springframework.data.domain.Pageable pageable) {
            List<Meetup> filtered = storage.values().stream()
                .filter(meetup -> meetup.getCategory().equals(category))
                .toList();
            return new org.springframework.data.domain.PageImpl<>(filtered);
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findByOwner_Id(UUID ownerId, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findBySearchKeyword(String search, org.springframework.data.domain.Pageable pageable) {
            List<Meetup> filtered = storage.values().stream()
                .filter(meetup -> meetup.getName().toLowerCase().contains(search.toLowerCase()) || 
                                 meetup.getDescription().toLowerCase().contains(search.toLowerCase()))
                .toList();
            return new org.springframework.data.domain.PageImpl<>(filtered);
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findByFilters(String category, String status, String search, Integer scoreLimit, LocalDateTime startDate, LocalDateTime endDate, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findByLocationRadius(Double latitude, Double longitude, Double radiusMeters, String category, String status, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findByTag(String tag, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findByTags(String[] tags, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findByStartAtBetween(LocalDateTime startDate, LocalDateTime endDate, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        // 나머지 Spring Data JPA 기본 메서드들
        @Override
        public <S extends Meetup> List<S> saveAll(Iterable<S> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<Meetup> findAllById(Iterable<UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteById(UUID id) {
            storage.remove(id);
        }

        @Override
        public void delete(Meetup entity) {
            if (entity != null && entity.getId() != null) {
                storage.remove(entity.getId());
            }
        }

        @Override
        public void deleteAllById(Iterable<? extends UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAll(Iterable<? extends Meetup> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<Meetup> findAll(org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Meetup> findAll(org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup> long count(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup> boolean exists(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup> List<S> findAll(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup> S saveAndFlush(S entity) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Meetup> List<S> saveAllAndFlush(Iterable<S> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllInBatch(Iterable<Meetup> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllInBatch() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Meetup getOne(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Meetup getById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Meetup getReferenceById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    // Member Mock
    static class MockMemberRepository implements MemberRepository {
        private final Map<UUID, Member> storage = new HashMap<>();

        @Override
        public <S extends Member> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Member> findById(UUID id) {
            return Optional.ofNullable(storage.get(id));
        }

        // 필요한 메서드들만 구현하고 나머지는 나중에
        @Override
        public boolean existsById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public boolean existsByEmail(String email) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Optional<Member> findMemberByEmail(String email) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        // Spring Data JPA 기본 메서드들도 일단 UnsupportedOperationException
        @Override
        public List<Member> findAll() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> List<S> saveAll(Iterable<S> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<Member> findAllById(Iterable<UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void delete(Member entity) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllById(Iterable<? extends UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAll(Iterable<? extends Member> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<Member> findAll(org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Member> findAll(org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> long count(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> boolean exists(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> List<S> findAll(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> S saveAndFlush(S entity) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Member> List<S> saveAllAndFlush(Iterable<S> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllInBatch(Iterable<Member> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllInBatch() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Member getOne(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Member getById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Member getReferenceById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    // Profile Mock
    static class MockProfileRepository implements ProfileRepository {
        private final Map<UUID, Profile> storage = new HashMap<>();
        private final Map<UUID, Profile> memberIdIndex = new HashMap<>();

        @Override
        public <S extends Profile> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            storage.put(entity.getId(), entity);
            memberIdIndex.put(entity.getMemberId(), entity);
            return entity;
        }

        @Override
        public Optional<Profile> findByMemberId(UUID memberId) {
            return Optional.ofNullable(memberIdIndex.get(memberId));
        }

        @Override
        public Optional<Profile> findById(UUID id) {
            return Optional.ofNullable(storage.get(id));
        }

        // 나머지는 일단 UnsupportedOperationException
        @Override
        public boolean existsById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public boolean existsByMemberId(UUID memberId) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public boolean existsByNicknameIgnoreCase(String nickname) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<Profile> findAll() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> List<S> saveAll(Iterable<S> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<Profile> findAllById(Iterable<UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void delete(Profile entity) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllById(Iterable<? extends UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAll(Iterable<? extends Profile> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<Profile> findAll(org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public org.springframework.data.domain.Page<Profile> findAll(org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> long count(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> boolean exists(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> List<S> findAll(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> S saveAndFlush(S entity) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public <S extends Profile> List<S> saveAllAndFlush(Iterable<S> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllInBatch(Iterable<Profile> entities) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> ids) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public void deleteAllInBatch() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Profile getOne(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Profile getById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Profile getReferenceById(UUID id) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
}
