package com.pnu.momeet.e2e.participant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.block.repository.BlockRepository;
import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ParticipantVisableListTest extends BaseParticipantTest {

    @Autowired
    private BlockRepository blockRepository;

    @Test
    @DisplayName("차단 관계가 없으면 전체와 동일하게 보인다 - 200 OK")
    void getVisibleParticipants_noBlock_sameAsAll_success() {
        // 밋업 생성 및 2명 참가
        MeetupDetail meetup = createTestMeetup(0);
        for (int i = 1; i < 3; i++) {
            given()
                .header(AUTH_HEADER, BEAR_PREFIX + tokens.get(i).accessToken())
                .pathParam("meetupId", meetup.id())
                .when()
                .post("/{meetupId}/participants")
                .then()
                .statusCode(200);
        }

        // host(0) 기준 visible 조회 → 3명(Host + 2명)
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + tokens.getFirst().accessToken())
            .pathParam("meetupId", meetup.id())
            .when()
            .get("/{meetupId}/participants/visible")
            .then()
            .log().all()
            .statusCode(200)
            .body("size()", equalTo(3))
            .body("profile.nickname", hasItems(
                profiles.get(0).getNickname(),
                profiles.get(1).getNickname(),
                profiles.get(2).getNickname()
            ));
    }

    @Test
    @DisplayName("viewer가 특정 참가자를 차단하면 해당 참가자는 제외된다 - 200 OK")
    void getVisibleParticipants_viewerBlocksTarget_excluded_success() {
        // 밋업 생성 + 2명 참가
        MeetupDetail meetup = createTestMeetup(0);
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + tokens.get(1).accessToken())
            .pathParam("meetupId", meetup.id())
            .when()
            .post("/{meetupId}/participants")
            .then()
            .statusCode(200);
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + tokens.get(2).accessToken())
            .pathParam("meetupId", meetup.id())
            .when()
            .post("/{meetupId}/participants")
            .then()
            .statusCode(200);

        // viewer(0) -> target(1) 차단 생성
        blockRepository.save(UserBlock.create(members.get(0).id(), members.get(1).id()));

        // host(0) 기준 visible 조회 → 1번은 제외, 0/2만 보임
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + tokens.get(0).accessToken())
            .pathParam("meetupId", meetup.id())
            .when()
            .get("/{meetupId}/participants/visible")
            .then()
            .log().all()
            .statusCode(200)
            .body("profile.nickname", hasItems(
                profiles.get(0).getNickname(),
                profiles.get(2).getNickname()
            ))
            .body("profile.nickname", not(hasItem(profiles.get(1).getNickname())));
    }

    @Test
    @DisplayName("[visible] 대상이 viewer를 차단해도 대상은 제외된다(역방향 차단) - 200 OK")
    void getVisibleParticipants_targetBlocksViewer_excluded_success() {
        // 밋업 생성 + 2명 참가
        MeetupDetail meetup = createTestMeetup(0);
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + tokens.get(1).accessToken())
            .pathParam("meetupId", meetup.id())
            .when()
            .post("/{meetupId}/participants")
            .then()
            .statusCode(200);

        // target(1) -> viewer(0) 역방향 차단
        blockRepository.save(UserBlock.create(members.get(1).id(), members.get(0).id()));

        // host(0) 기준 visible 조회 → 1번은 제외, host만 보임
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + tokens.get(0).accessToken())
            .pathParam("meetupId", meetup.id())
            .when()
            .get("/{meetupId}/participants/visible")
            .then()
            .log().all()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].profile.nickname", equalTo(profiles.getFirst().getNickname()));
    }

    @Test
    @DisplayName("다른 참가자 시점에서도 자신의 차단 대상은 보이지 않는다 - 200 OK")
    void getVisibleParticipants_fromParticipantPerspective_success() {
        // 밋업 생성 + 2명 참가 (1,2)
        MeetupDetail meetup = createTestMeetup(0);
        for (int i = 1; i <= 2; i++) {
            given()
                .header(AUTH_HEADER, BEAR_PREFIX + tokens.get(i).accessToken())
                .pathParam("meetupId", meetup.id())
                .when()
                .post("/{meetupId}/participants")
                .then()
                .statusCode(200);
        }

        // viewer(2) -> target(host=0) 차단
        blockRepository.save(UserBlock.create(members.get(2).id(), members.get(0).id()));

        // 2번 시점에서 visible 조회 → host(0) 제외, 1/2만 보임
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + tokens.get(2).accessToken())
            .pathParam("meetupId", meetup.id())
            .when()
            .get("/{meetupId}/participants/visible")
            .then()
            .log().all()
            .statusCode(200)
            .body("profile.nickname", hasItems(
                profiles.get(1).getNickname(),
                profiles.get(2).getNickname()
            ))
            .body("profile.nickname", not(hasItem(profiles.get(0).getNickname())));
    }

    @Test
    @DisplayName("존재하지 않는 모임 ID는 404 - Not Found")
    void getVisibleParticipants_nonExistentMeetup_fail() {
        String non = "00000000-0000-0000-0000-000000000000";
        given()
            .header(AUTH_HEADER, BEAR_PREFIX + tokens.getFirst().accessToken())
            .pathParam("meetupId", non)
            .when()
            .get("/{meetupId}/participants/visible")
            .then()
            .log().all()
            .statusCode(404);
    }

    @Test
    @DisplayName("미인증 사용자는 401 - Unauthorized")
    void getVisibleParticipants_unauthenticated_fail() {
        MeetupDetail meetup = createTestMeetup(0);
        given()
            .pathParam("meetupId", meetup.id())
            .when()
            .get("/{meetupId}/participants/visible")
            .then()
            .log().all()
            .statusCode(401);
    }
}
