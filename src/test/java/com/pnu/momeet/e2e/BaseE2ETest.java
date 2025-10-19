package com.pnu.momeet.e2e;


import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("e2e")
@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class BaseE2ETest {

    @Value("${local.server.port}")
    protected int port;

    public static final String TEST_ADMIN_EMAIL = "admin@test.com";
    public static final String TEST_ADMIN_PASSWORD = "testpass1212!";

    public static final String TEST_USER_EMAIL = "user@test.com";
    public static final String TEST_USER_PASSWORD = "testpass1212!";

    public static final String TEST_ALICE_USER_EMAIL = "alice@test.com";
    public static final String TEST_ALICE_USER_PASSWORD = "testpass1212!";

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEAR_PREFIX = "Bearer ";

    @BeforeEach
    protected void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
