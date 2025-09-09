package com.pnu.momeet.e2e.sigungu;

import com.pnu.momeet.domain.sigungu.dto.response.SigunguResponse;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Tag("read")
@DisplayName("E2E 테스트 : 시군구 조회")
public class SigunguReadTest extends BaseSigunguTest {

    @Test
    @DisplayName("시군구 목록 조회 성공 테스트")
    public void sigungu_read_list_success() {
        List<SigunguResponse> responses =  RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .queryParam("size", 5) // 페이지 사이즈 5
                .queryParam("page", 1) // 페이지 번호 1
                .queryParam("sort", "sidoName,DESC") // 시도명 내림차순
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "content.size()", equalTo(5),
                    "page.size", equalTo(5),
                    "page.number", equalTo(1),
                    "page.totalElements", equalTo(252), // 전체 시군구 개수 = 252
                    "page.totalPages", equalTo(51) // 252 / 5 = 50.4 -> 올림 = 51
                )
            .extract()
                .body()
                .jsonPath()
                .getList("content", SigunguResponse.class);

        // 시도명이 내림차순으로 정렬되었는지 검증
        for (int i = 1; i < responses.size(); i++) {
            String prevSidoName = responses.get(i - 1).sidoName();
            String currSidoName = responses.get(i).sidoName();
            // 내림차순 검증
            assertThat(prevSidoName.compareTo(currSidoName) >= 0);
        }
    }

    @Test
    @DisplayName("시군구 필터링 성공 테스트 - sidoCode")
    public void sigungu_read_list_success_with_filters() {
        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .queryParam("sidoCode", "11") // 서울특별시
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "content.size()", equalTo(10),
                    "page.size", equalTo(10), // 기본 페이지 사이즈 = 10
                    "page.number", equalTo(0), // 기본 페이지 번호 = 0
                    "page.totalElements", equalTo(25), // 서울특별시의 시군구 개수 = 25
                    "page.totalPages", equalTo(3)
                );
    }

    @Test
    @DisplayName("시군구 단건 조회 성공 테스트")
    public void sigungu_read_one_success() {
        Long sigunguId = 11680L; // 강남구

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
            .when()
                .get("/{id}", sigunguId)
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "sidoCode", equalTo(11),
                    "sidoName", equalTo("서울특별시"),
                    "sigunguCode", equalTo(11680),
                    "sigunguName", equalTo("강남구")
                );
    }

    @Test
    @DisplayName("시군구 조회 성공 테스트: 위도 경로를 통해 조회")
    public void sigungu_read_one_success_with_location() {
        // 대한민국 서울특별시 송파구 올림픽로 - 롯데타워 위치
        double latitude =37.5124641;
        double longitude =  127.102543;

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
            .when()
                .get("/within")
            .then()
                .log().all()
                .statusCode(200)
                .body(
                    "sidoCode", equalTo(11),
                    "sidoName", equalTo("서울특별시"),
                    "sigunguCode", equalTo(11710),
                    "sigunguName", equalTo("송파구")
                );
    }

    @Test
    @DisplayName("시군구 단건 조회 실패 테스트 - 존재하지 않는 시군구")
    public void sigungu_read_one_fail_not_found() {
        Long sigunguId = 99999L; // 존재하지 않는 시군구 코드

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
            .when()
                .get("/{id}", sigunguId)
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("시군구 조회 실패 테스트 - 좌표가 대한민국 내에 없음")
    public void sigungu_read_one_fail_location_not_found() {
        // 미국 뉴욕 타임스퀘어 위치
        double latitude =40.7580;
        double longitude =  -73.9855;

        RestAssured
            .given()
                .header(AUTH_HEADER, BEAR_PREFIX + getToken().accessToken())
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
            .when()
                .get("/within")
            .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("시군구 목록 조회 실패 테스트 - 인증 실패")
    public void sigungu_read_list_fail_unauthorized() {
        RestAssured
            .given()
            .when()
                .get()
            .then()
                .log().all()
                .statusCode(401);
    }
}
