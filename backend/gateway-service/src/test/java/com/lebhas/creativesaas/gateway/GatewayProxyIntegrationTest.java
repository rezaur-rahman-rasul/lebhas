package com.lebhas.creativesaas.gateway;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import tools.jackson.databind.ObjectMapper;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayProxyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static HttpServer authServer;
    private static HttpServer workspaceServer;
    private static HttpServer creativeServer;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        ensureMockServers();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("platform.gateway.services.auth", () -> "http://localhost:" + authServer.getAddress().getPort());
        registry.add("platform.gateway.services.workspace", () -> "http://localhost:" + workspaceServer.getAddress().getPort());
        registry.add("platform.gateway.services.creative", () -> "http://localhost:" + creativeServer.getAddress().getPort());
    }

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UserEntity masterUser;

    @BeforeAll
    static void beforeAll() throws IOException {
        ensureMockServers();
    }

    @AfterAll
    static void afterAll() {
        stop(authServer);
        stop(workspaceServer);
        stop(creativeServer);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        masterUser = userRepository.save(UserEntity.register(
                "Master",
                "User",
                "master@example.com",
                null,
                "{noop}unused",
                Role.MASTER,
                UserStatus.ACTIVE,
                true));
    }

    @Test
    void shouldRouteWorkspaceRequestsAndPropagateCorrelationId() {
        String accessToken = jwtAccessTokenService.generate(masterUser, null, Role.MASTER).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Correlation-ID", "corr-123")
                .when()
                .get("/api/v1/workspaces/demo-workspace")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", equalTo("corr-123"))
                .body("service", equalTo("workspace"))
                .body("correlationId", equalTo("corr-123"))
                .body("path", equalTo("/api/v1/workspaces/demo-workspace"));
    }

    @Test
    void shouldRouteCreativeAssetRequests() {
        String accessToken = jwtAccessTokenService.generate(masterUser, null, Role.MASTER).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/workspaces/demo/assets")
                .then()
                .statusCode(200)
                .body("service", equalTo("creative"));
    }

    @Test
    void shouldProxyPublicAuthRequests() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "user@example.com"))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(202)
                .body("service", equalTo("auth"))
                .body("method", equalTo("POST"));
    }

    private static void ensureMockServers() throws IOException {
        if (authServer == null) {
            authServer = createServer("auth", 202);
            workspaceServer = createServer("workspace", 200);
            creativeServer = createServer("creative", 200);
        }
    }

    private static HttpServer createServer(String serviceName, int statusCode) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", exchange -> respond(serviceName, statusCode, exchange));
        server.start();
        return server;
    }

    private static void respond(String serviceName, int statusCode, HttpExchange exchange) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", serviceName);
        payload.put("method", exchange.getRequestMethod());
        payload.put("path", exchange.getRequestURI().getPath());
        payload.put("correlationId", exchange.getRequestHeaders().getFirst("X-Correlation-ID"));
        payload.put("body", new String(requestBody, StandardCharsets.UTF_8));

        byte[] responseBody = new ObjectMapper().writeValueAsBytes(payload);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }

    private static void stop(HttpServer server) {
        if (server != null) {
            server.stop(0);
        }
    }
}
