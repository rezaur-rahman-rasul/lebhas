package com.lebhas.creativesaas.auth;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.RefreshTokenRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthSecurityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("platform.security.rate-limit.login.lockout-threshold", () -> 3);
        registry.add("platform.security.rate-limit.login.max-attempts-per-identity", () -> 20);
        registry.add("platform.security.rate-limit.refresh.max-attempts-per-token", () -> 2);
        registry.add("platform.security.rate-limit.refresh.max-attempts-per-ip", () -> 20);
    }

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        UserEntity user = UserEntity.register(
                "Master",
                "User",
                "master@example.com",
                null,
                passwordEncoder.encode("CorrectPassword!1"),
                Role.MASTER,
                UserStatus.ACTIVE,
                true);
        userRepository.save(user);
    }

    @Test
    void shouldLockAccountAfterRepeatedFailedLogins() {
        Map<String, Object> request = Map.of(
                "email", "master@example.com",
                "password", "WrongPassword!1");

        for (int attempt = 0; attempt < 3; attempt++) {
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/auth/login")
                    .then()
                    .statusCode(401)
                    .body("success", equalTo(false));
        }

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "master@example.com",
                        "password", "CorrectPassword!1"))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(429)
                .body("errors[0].code", equalTo("AUTH-429-01"));
    }

    @Test
    void shouldRateLimitRepeatedRefreshTokenAbuse() {
        Map<String, Object> request = Map.of("refreshToken", "invalid.token");

        for (int attempt = 0; attempt < 2; attempt++) {
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(401)
                    .body("success", equalTo(false));
        }

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(429)
                .body("errors[0].code", equalTo("AUTH-429-01"));
    }
}
