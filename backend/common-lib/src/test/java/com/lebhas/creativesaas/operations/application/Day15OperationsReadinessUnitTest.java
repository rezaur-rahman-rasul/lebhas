package com.lebhas.creativesaas.operations.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.operations.domain.GoLiveChecklistItem;
import com.lebhas.creativesaas.operations.domain.GoLiveChecklistItemStatus;
import com.lebhas.creativesaas.operations.domain.SmokeTestRun;
import com.lebhas.creativesaas.operations.domain.SystemFeatureToggle;
import com.lebhas.creativesaas.operations.domain.SystemFeatureToggleKey;
import com.lebhas.creativesaas.operations.infrastructure.persistence.DataIntegrityRunRepository;
import com.lebhas.creativesaas.operations.infrastructure.persistence.GoLiveChecklistItemRepository;
import com.lebhas.creativesaas.operations.infrastructure.persistence.SmokeTestRunRepository;
import com.lebhas.creativesaas.operations.infrastructure.persistence.SystemFeatureToggleRepository;
import com.lebhas.creativesaas.operations.web.MaintenanceModeInterceptor;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Day15OperationsReadinessUnitTest {
    private final SystemFeatureToggleRepository toggleRepository = mock(SystemFeatureToggleRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final SmokeTestRunRepository smokeRepository = mock(SmokeTestRunRepository.class);
    private final GoLiveChecklistItemRepository checklistRepository = mock(GoLiveChecklistItemRepository.class);
    private final DataIntegrityRunRepository integrityRepository = mock(DataIntegrityRunRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<RedisLockService> lockProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher> eventProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<RedisRateLimitService> rateProvider = mock(ObjectProvider.class);

    private SystemFeatureToggleService toggleService;
    private OperationsReadinessService readinessService;

    @BeforeEach
    void setUp() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        toggleService = new SystemFeatureToggleService(toggleRepository, redisProvider);
        readinessService = new OperationsReadinessService(toggleService, smokeRepository, checklistRepository, integrityRepository,
                jdbcTemplate, lockProvider, eventProvider, "short-secret", "jdbc:postgresql://safe/db", "localhost");
        when(eventProvider.getIfAvailable()).thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);
    }

    @Test
    void masterUpdatesToggleAndInvalidatesRedisCache() {
        when(toggleRepository.findByToggleKeyAndDeletedFalse(SystemFeatureToggleKey.AI_GENERATION_ENABLED)).thenReturn(Optional.empty());
        when(toggleRepository.save(any(SystemFeatureToggle.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        var view = toggleService.update(SystemFeatureToggleKey.AI_GENERATION_ENABLED, false, "incident");

        assertThat(view.enabled()).isFalse();
        verify(redis).delete("ops:feature-toggle:AI_GENERATION_ENABLED");
        verify(toggleRepository).save(any(SystemFeatureToggle.class));
    }

    @Test
    void toggleReadsAndWritesRedisCache() {
        when(valueOps.get("ops:feature-toggle:TEXT_TOOLS_ENABLED")).thenReturn(null);
        when(toggleRepository.findByToggleKeyAndDeletedFalse(SystemFeatureToggleKey.TEXT_TOOLS_ENABLED))
                .thenReturn(Optional.of(withId(SystemFeatureToggle.create(SystemFeatureToggleKey.TEXT_TOOLS_ENABLED, false, null), UUID.randomUUID())));

        assertThat(toggleService.isEnabled(SystemFeatureToggleKey.TEXT_TOOLS_ENABLED)).isFalse();

        verify(valueOps).set(eq("ops:feature-toggle:TEXT_TOOLS_ENABLED"), eq("false"), any(Duration.class));
    }

    @Test
    void maintenanceModeBlocksNonCriticalWrites() {
        MaintenanceModeInterceptor interceptor = new MaintenanceModeInterceptor(toggleService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/workspaces/abc/assets");
        when(valueOps.get("ops:feature-toggle:MAINTENANCE_MODE")).thenReturn("true");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maintenance mode");
    }

    @Test
    void smokeTestPersistsResults() {
        when(lockProvider.getIfAvailable()).thenReturn(null);
        when(smokeRepository.save(any(SmokeTestRun.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        var run = readinessService.runSmokeTests();

        assertThat(run.results()).containsEntry("realAiCalled", false);
        assertThat(run.results()).containsEntry("realPaymentCalled", false);
        verify(smokeRepository, atLeastOnce()).save(any(SmokeTestRun.class));
    }

    @Test
    void duplicateSmokeTestBlockedByRedisLock() {
        RedisLockService lockService = mock(RedisLockService.class);
        when(lockProvider.getIfAvailable()).thenReturn(lockService);
        when(lockService.acquire(eq("ops:smoke-test:run"), any(Duration.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readinessService.runSmokeTests())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already running");
    }

    @Test
    void goLiveReadinessFalseWhenItemsPending() {
        GoLiveChecklistItem item = withId(GoLiveChecklistItem.create("DNS", "verify"), UUID.randomUUID());
        when(checklistRepository.findAllByDeletedFalseOrderByCreatedAtAsc()).thenReturn(List.of(item));

        var readiness = readinessService.goLiveReadiness();

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.pendingItems()).isEqualTo(1);
    }

    @Test
    void securityReadinessDetectsUnsafeConfigWithoutExposingSecret() {
        var security = readinessService.securityReadiness();

        assertThat(security.ready()).isFalse();
        assertThat(security.checks().toString()).doesNotContain("short-secret");
    }

    @Test
    void rateLimitBlocksRepeatedAttempts() {
        RedisRateLimitService rateService = mock(RedisRateLimitService.class);
        when(rateProvider.getIfAvailable()).thenReturn(rateService);
        when(rateService.increment(anyString(), anyLong(), any(Duration.class)))
                .thenReturn(new RedisRateLimitService.RateLimitWindow(11, 10, false, Duration.ofMinutes(1)));
        OperationalRateLimitService service = new OperationalRateLimitService(rateProvider);

        assertThatThrownBy(() -> service.check(OperationalRateLimitService.Operation.LOGIN, "ip"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    void dataIntegrityDetectsBrokenHierarchy() {
        when(integrityRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L, 0L, 2L, 0L, 0L, 0L, 0L, 0L, 0L);

        var run = readinessService.runDataIntegrity();

        assertThat(run.issueCount()).isGreaterThan(0);
        assertThat(run.results()).containsKey("productServiceWithoutBrand");
    }

    @Test
    void operationalOverviewReturnsSafeStatus() {
        var overview = readinessService.overview();

        assertThat(overview.status().toString()).doesNotContain("secret", "password", "token");
        assertThat(overview.status()).containsKey("databaseConfigured");
    }

    @Test
    void noRabbitMqLocalFilesystemOrHardcodedPackageNames() throws Exception {
        Path source = Path.of("backend/common-lib/src/main/java/com/lebhas/creativesaas/operations/application/OperationsReadinessService.java");
        if (!Files.exists(source)) {
            source = Path.of("src/main/java/com/lebhas/creativesaas/operations/application/OperationsReadinessService.java");
        }
        String text = Files.readString(source);

        assertThat(text).doesNotContain("RabbitMQ", "rabbitmq", "StorageProvider.LOCAL", "java.nio.file", "Files.", "Path.");
        assertThat(text).doesNotContain("FREE", "BASIC_PLAN", "PREMIUM_PLAN", "ENTERPRISE");
    }

    private <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        setFieldIfPresent(entity, "createdAt", Instant.parse("2026-06-01T00:00:00Z"));
        setFieldIfPresent(entity, "updatedAt", Instant.parse("2026-06-01T00:00:00Z"));
        return entity;
    }

    private void setFieldIfPresent(Object target, String fieldName, Object value) {
        try {
            ReflectionTestUtils.setField(target, fieldName, value);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
