package com.lebhas.creativesaas.workspace;

import com.lebhas.creativesaas.redis.RedisAiPromptCache;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisGenerationDeduplicationLock;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisPermissionCache;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import com.lebhas.creativesaas.redis.RedisSessionService;
import com.lebhas.creativesaas.redis.RedisSignedUrlCache;
import com.lebhas.creativesaas.redis.RedisWalletCache;
import com.lebhas.creativesaas.redis.RedisWorkspaceContextCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RedisFoundationIntegrationTest {

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

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private RedisKeyBuilder redisKeyBuilder;

    @Autowired
    private RedisLockService redisLockService;

    @Autowired
    private RedisRateLimitService redisRateLimitService;

    @Autowired
    private RedisSessionService redisSessionService;

    @Autowired
    private RedisWorkspaceContextCache redisWorkspaceContextCache;

    @Autowired
    private RedisPermissionCache redisPermissionCache;

    @Autowired
    private RedisWalletCache redisWalletCache;

    @Autowired
    private RedisSignedUrlCache redisSignedUrlCache;

    @Autowired
    private RedisAiPromptCache redisAiPromptCache;

    @Autowired
    private RedisGenerationDeduplicationLock redisGenerationDeduplicationLock;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void shouldSupportRedisFoundationOperations() throws Exception {
        redisCacheService.set("foundation:test", new CacheValue("cached"), Duration.ofMillis(500));
        assertThat(redisCacheService.get("foundation:test", CacheValue.class))
                .contains(new CacheValue("cached"));
        Thread.sleep(750L);
        assertThat(redisCacheService.get("foundation:test", CacheValue.class)).isEmpty();

        assertThat(redisKeyBuilder.authRefresh("token-1")).isEqualTo("auth:refresh:token-1");
        assertThat(redisKeyBuilder.authSession(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "device-1"))
                .isEqualTo("auth:session:00000000-0000-0000-0000-000000000001:device-1");
        assertThat(redisKeyBuilder.workspaceContext(
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000011")))
                .isEqualTo("workspace:context:00000000-0000-0000-0000-000000000010:00000000-0000-0000-0000-000000000011");
        assertThat(redisKeyBuilder.permissions(
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000011")))
                .isEqualTo("permissions:00000000-0000-0000-0000-000000000010:00000000-0000-0000-0000-000000000011");

        String lockKey = "lock:test";
        RedisLockService.RedisLockToken token = redisLockService.acquire(lockKey, Duration.ofSeconds(5)).orElseThrow();
        assertThat(redisLockService.acquire(lockKey, Duration.ofSeconds(5))).isEmpty();
        assertThat(redisLockService.release(token)).isTrue();
        assertThat(redisLockService.acquire(lockKey, Duration.ofSeconds(5))).isPresent();

        RedisRateLimitService.RateLimitWindow firstWindow = redisRateLimitService.increment("rate:test", 2, Duration.ofMinutes(1));
        RedisRateLimitService.RateLimitWindow secondWindow = redisRateLimitService.increment("rate:test", 2, Duration.ofMinutes(1));
        RedisRateLimitService.RateLimitWindow thirdWindow = redisRateLimitService.increment("rate:test", 2, Duration.ofMinutes(1));
        assertThat(firstWindow.currentCount()).isEqualTo(1);
        assertThat(secondWindow.currentCount()).isEqualTo(2);
        assertThat(thirdWindow.allowed()).isFalse();

        redisSessionService.blacklistAccessToken("jwt-1", Duration.ofMinutes(1));
        assertThat(redisSessionService.isAccessTokenBlacklisted("jwt-1")).isTrue();

        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RedisWorkspaceContextCache.WorkspaceContextSnapshot workspaceContextSnapshot =
                new RedisWorkspaceContextCache.WorkspaceContextSnapshot(
                        workspaceId,
                        userId,
                        "CREW",
                        Set.of("WORKSPACE_VIEW"),
                        true,
                        false,
                        Instant.now());
        redisWorkspaceContextCache.getOrLoad(workspaceId, userId, () -> workspaceContextSnapshot);
        assertThat(redisWorkspaceContextCache.get(workspaceId, userId))
                .contains(workspaceContextSnapshot);

        RedisPermissionCache.PermissionSnapshot permissionSnapshot =
                new RedisPermissionCache.PermissionSnapshot(workspaceId, userId, Set.of("WORKSPACE_VIEW", "CREATIVE_DOWNLOAD"), 1L, Instant.now());
        redisPermissionCache.getOrLoad(workspaceId, userId, 1L, () -> permissionSnapshot);
        assertThat(redisPermissionCache.get(workspaceId, userId))
                .contains(permissionSnapshot);

        RedisWalletCache.WalletSnapshot walletSnapshot =
                new RedisWalletCache.WalletSnapshot(new BigDecimal("80.0000"), new BigDecimal("20.0000"), Instant.now());
        redisWalletCache.store(workspaceId, walletSnapshot);
        assertThat(redisWalletCache.get(workspaceId)).contains(walletSnapshot);

        UUID storageFileId = UUID.randomUUID();
        RedisSignedUrlCache.SignedUrlSnapshot signedUrlSnapshot =
                new RedisSignedUrlCache.SignedUrlSnapshot(
                        "https://cdn.example.com/file",
                        Instant.now().plusSeconds(60),
                        "download",
                        "https://cdn.example.com/file",
                        Instant.now());
        redisSignedUrlCache.store(storageFileId, signedUrlSnapshot, Duration.ofSeconds(30));
        assertThat(redisSignedUrlCache.get(storageFileId)).contains(signedUrlSnapshot);

        String promptHash = redisAiPromptCache.hash("Generate a launch poster");
        RedisAiPromptCache.PromptCacheValue promptCacheValue =
                new RedisAiPromptCache.PromptCacheValue(
                        promptHash,
                        "stub-provider",
                        "stub-model",
                        "{\"headline\":\"Launch\"}",
                        Instant.now());
        redisAiPromptCache.store(promptHash, promptCacheValue);
        assertThat(redisAiPromptCache.get(promptHash)).contains(promptCacheValue);

        RedisLockService.RedisLockToken dedupToken =
                redisGenerationDeduplicationLock.acquire("duplicate-request", Duration.ofSeconds(10)).orElseThrow();
        assertThat(redisGenerationDeduplicationLock.acquire("duplicate-request", Duration.ofSeconds(10))).isEmpty();
        assertThat(redisGenerationDeduplicationLock.release(dedupToken)).isTrue();

        AtomicInteger loadCounter = new AtomicInteger();
        CacheValue loaded = redisCacheService.getOrLoad(
                "foundation:source-of-truth",
                Duration.ofMinutes(1),
                CacheValue.class,
                () -> new CacheValue("load-" + loadCounter.incrementAndGet()));
        CacheValue cached = redisCacheService.getOrLoad(
                "foundation:source-of-truth",
                Duration.ofMinutes(1),
                CacheValue.class,
                () -> new CacheValue("load-" + loadCounter.incrementAndGet()));
        assertThat(loaded.value()).isEqualTo("load-1");
        assertThat(cached.value()).isEqualTo("load-1");
        assertThat(loadCounter.get()).isEqualTo(1);
    }

    record CacheValue(String value) {
    }
}
