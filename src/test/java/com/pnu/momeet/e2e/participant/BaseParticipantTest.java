package com.pnu.momeet.e2e.participant;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.service.EmailAuthService;
import com.pnu.momeet.domain.meetup.dto.request.LocationRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import com.pnu.momeet.domain.meetup.service.MeetupDomainService;
import com.pnu.momeet.domain.member.dto.request.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberDomainService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import com.pnu.momeet.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class BaseParticipantTest extends BaseE2ETest {
    protected static final int TEST_USER_COUNT = 5;

    protected List<String> emails;
    protected List<MemberResponse> members;
    protected List<Profile> profiles;
    protected List<TokenResponse> tokens;
    protected Map<Integer, MeetupDetail> meetups;

    @Autowired
    protected EmailAuthService emailAuthService;
    @Autowired
    protected MemberDomainService memberService;
    @Autowired
    protected ProfileRepository profileRepository;
    @Autowired
    protected MeetupDomainService meetupService;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        RestAssured.basePath = "/api/meetups";

        emails = new ArrayList<>();
        members = new ArrayList<>();
        profiles = new ArrayList<>();
        tokens = new ArrayList<>();
        meetups = new HashMap<>();

        for (int i = 0; i < TEST_USER_COUNT; i++) {
            createTestcase();
        }
    }

    @AfterEach
    protected void teardown() {
        // 먼저 모든 meetup 삭제
        if (meetups != null) {
            for (MeetupDetail meetup : meetups.values()) {
                try {
                    meetupService.deleteMeetupAdmin(meetup.id());
                } catch (Exception e) {
                    // 이미 삭제되었거나 오류가 발생한 경우 무시
                }
            }
            meetups.clear();
        }
        
        // 그 다음 profile 삭제
        if (profiles != null) {
            for (int i = profiles.size() - 1; i >= 0; i--) {
                try {
                    profileRepository.delete(profiles.get(i));
                } catch (Exception e) {
                    // 이미 삭제되었거나 오류가 발생한 경우 무시
                }
            }
            profiles.clear();
        }
        
        // 마지막으로 member 삭제
        if (members != null) {
            for (int i = members.size() - 1; i >= 0; i--) {
                try {
                    memberService.deleteMemberById(members.get(i).id());
                } catch (Exception e) {
                    // 이미 삭제되었거나 오류가 발생한 경우 무시
                }
            }
            members.clear();
        }
        
        if (emails != null) emails.clear();
        if (tokens != null) tokens.clear();
    }

    protected void createTestcase() {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        emails.add(email);
        members.add(memberService.saveMember(new MemberCreateRequest(
                email, TEST_USER_PASSWORD, List.of(Role.ROLE_USER.name())
        )));
        tokens.add(emailAuthService.login(email, TEST_USER_PASSWORD));
        profiles.add(profileRepository.save(Profile.create(
                members.getLast().id(),
                UUID.randomUUID().toString().substring(0, 5) + "User",
                25,
                Gender.MALE,
                "www.example.com/image.png",
                "테스트 소개",
                "부산시 금정구"
        )));
    }

    protected MeetupDetail createTestMeetup(int index) {
        MeetupCreateRequest request = new MeetupCreateRequest(
                "테스트 밋업 " + index,
                "STUDY",
                "CERTIFICATE",
                "테스트 밋업 설명",
                List.of("테스트", "밋업", "해시태그" + index),
                10,
                10.0,
                10,
                new LocationRequest(35.243322, 129.088287, "부산시 금정구")
        );

        MeetupDetail meetup = meetupService.createMeetup(request, members.get(index).id());
        meetups.put(index, meetup);
        return meetup;
    }

}
