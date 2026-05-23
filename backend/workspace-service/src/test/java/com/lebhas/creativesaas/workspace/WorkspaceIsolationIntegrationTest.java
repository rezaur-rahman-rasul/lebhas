package com.lebhas.creativesaas.workspace;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkspaceIsolationIntegrationTest {

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
    }

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private WorkspaceEntity workspaceOne;
    private WorkspaceEntity workspaceTwo;
    private UserEntity adminUser;
    private UserEntity masterUser;

    @BeforeEach
    void setUp() {
        workspaceMembershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        adminUser = userRepository.save(UserEntity.register(
                "Admin",
                "User",
                "admin@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));
        masterUser = userRepository.save(UserEntity.register(
                "Master",
                "User",
                "master@example.com",
                null,
                "{noop}unused",
                Role.MASTER,
                UserStatus.ACTIVE,
                true));

        workspaceOne = workspaceRepository.save(WorkspaceEntity.create(
                "Workspace One",
                "workspace-one",
                null,
                null,
                null,
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                adminUser.getId()));
        workspaceTwo = workspaceRepository.save(WorkspaceEntity.create(
                "Workspace Two",
                "workspace-two",
                null,
                null,
                null,
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                masterUser.getId()));

        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspaceOne.getId(),
                adminUser.getId(),
                Role.ADMIN,
                WorkspaceMembershipStatus.ACTIVE,
                java.util.Set.of(),
                Instant.now(),
                masterUser.getId()));
    }

    @Test
    void shouldBlockAdminFromAccessingAnotherWorkspace() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}", workspaceTwo.getId())
                .then()
                .statusCode(403)
                .body("errors[0].code", equalTo("TENANT-403"));
    }

    @Test
    void shouldAllowMasterToAccessAnyWorkspace() {
        String accessToken = jwtAccessTokenService.generate(masterUser, null, Role.MASTER).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}", workspaceTwo.getId())
                .then()
                .statusCode(200)
                .body("data.id", equalTo(workspaceTwo.getId().toString()));
    }
}
