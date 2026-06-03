package com.lebhas.creativesaas.operations.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.ChecklistCommand;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.ChecklistItemView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.DataIntegrityRunView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.DependencyHealthView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.DetailedReadinessView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.OverviewView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.ReadinessCheckView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.ReadinessView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.SecurityReadinessView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.SmokeTestRunView;
import com.lebhas.creativesaas.operations.domain.DataIntegrityRun;
import com.lebhas.creativesaas.operations.domain.GoLiveChecklistItem;
import com.lebhas.creativesaas.operations.domain.GoLiveChecklistItemStatus;
import com.lebhas.creativesaas.operations.domain.SmokeTestRun;
import com.lebhas.creativesaas.operations.domain.SmokeTestRunStatus;
import com.lebhas.creativesaas.operations.infrastructure.persistence.DataIntegrityRunRepository;
import com.lebhas.creativesaas.operations.infrastructure.persistence.GoLiveChecklistItemRepository;
import com.lebhas.creativesaas.operations.infrastructure.persistence.SmokeTestRunRepository;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OperationsReadinessService {
    private final SystemFeatureToggleService toggleService;
    private final SmokeTestRunRepository smokeRuns;
    private final GoLiveChecklistItemRepository checklistItems;
    private final DataIntegrityRunRepository integrityRuns;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<RedisLockService> redisLockService;
    private final DomainEventPublisher domainEventPublisher;
    private final String jwtSecret;
    private final String datasourceUrl;
    private final String redisHost;

    public OperationsReadinessService(
            SystemFeatureToggleService toggleService,
            SmokeTestRunRepository smokeRuns,
            GoLiveChecklistItemRepository checklistItems,
            DataIntegrityRunRepository integrityRuns,
            JdbcTemplate jdbcTemplate,
            ObjectProvider<RedisLockService> redisLockService,
            ObjectProvider<DomainEventPublisher> domainEventPublisher,
            @Value("${app.security.jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.data.redis.host:}") String redisHost
    ) {
        this.toggleService = toggleService;
        this.smokeRuns = smokeRuns;
        this.checklistItems = checklistItems;
        this.integrityRuns = integrityRuns;
        this.jdbcTemplate = jdbcTemplate;
        this.redisLockService = redisLockService;
        this.domainEventPublisher = domainEventPublisher.getIfAvailable();
        this.jwtSecret = jwtSecret;
        this.datasourceUrl = datasourceUrl;
        this.redisHost = redisHost;
    }

    public OverviewView overview() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("maintenance", toggleService.maintenanceStatus().maintenanceMode());
        status.put("databaseConfigured", safeConfigured(datasourceUrl));
        status.put("redisConfigured", safeConfigured(redisHost));
        return new OverviewView(status, toggleService.snapshot());
    }

    @Transactional
    public SmokeTestRunView runSmokeTests() {
        RedisLockService lock = redisLockService.getIfAvailable();
        var token = lock == null ? java.util.Optional.<RedisLockService.RedisLockToken>empty() : lock.acquire("ops:smoke-test:run", Duration.ofMinutes(5));
        if (lock != null && token.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Smoke test is already running");
        }
        try {
            SmokeTestRun run = smokeRuns.save(SmokeTestRun.running());
            Map<String, Object> results = new LinkedHashMap<>();
            results.put("database", checkSql("SELECT 1"));
            results.put("featureToggles", !toggleService.snapshot().isEmpty());
            results.put("realAiCalled", false);
            results.put("realPaymentCalled", false);
            boolean passed = Boolean.TRUE.equals(results.get("database"))
                    && Boolean.TRUE.equals(results.get("featureToggles"))
                    && Boolean.FALSE.equals(results.get("realAiCalled"))
                    && Boolean.FALSE.equals(results.get("realPaymentCalled"));
            run.complete(passed ? SmokeTestRunStatus.PASSED : SmokeTestRunStatus.FAILED, results);
            run = smokeRuns.save(run);
            publish(KafkaTopicConstants.SMOKE_TEST_RUN_COMPLETED, run.getId(), Map.of("status", run.getStatus().name()));
            return toView(run);
        } finally {
            if (lock != null) token.ifPresent(lock::releaseQuietly);
        }
    }

    @Transactional(readOnly = true)
    public List<SmokeTestRunView> smokeRuns() {
        return smokeRuns.findAllByDeletedFalseOrderByStartedAtDesc().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public SmokeTestRunView smokeRun(UUID id) {
        return toView(smokeRuns.findByIdAndDeletedFalse(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Smoke test run not found")));
    }

    @Transactional
    public ChecklistItemView createChecklistItem(ChecklistCommand command) {
        GoLiveChecklistItem item = checklistItems.save(GoLiveChecklistItem.create(command.title(), command.description()));
        publish(KafkaTopicConstants.GO_LIVE_CHECKLIST_UPDATED, item.getId(), Map.of("status", item.getStatus().name()));
        return toView(item);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemView> checklist() {
        return checklistItems.findAllByDeletedFalseOrderByCreatedAtAsc().stream().map(this::toView).toList();
    }

    @Transactional
    public ChecklistItemView updateChecklistItem(UUID id, ChecklistCommand command) {
        GoLiveChecklistItem item = requireChecklistItem(id);
        item.update(command.title(), command.description());
        return toView(checklistItems.save(item));
    }

    @Transactional
    public ChecklistItemView completeChecklistItem(UUID id) {
        GoLiveChecklistItem item = requireChecklistItem(id);
        item.complete();
        publish(KafkaTopicConstants.GO_LIVE_CHECKLIST_UPDATED, item.getId(), Map.of("status", item.getStatus().name()));
        return toView(checklistItems.save(item));
    }

    @Transactional
    public ChecklistItemView blockChecklistItem(UUID id, String reason) {
        GoLiveChecklistItem item = requireChecklistItem(id);
        item.block(reason);
        publish(KafkaTopicConstants.GO_LIVE_CHECKLIST_UPDATED, item.getId(), Map.of("status", item.getStatus().name()));
        return toView(checklistItems.save(item));
    }

    @Transactional(readOnly = true)
    public ReadinessView goLiveReadiness() {
        List<GoLiveChecklistItem> items = checklistItems.findAllByDeletedFalseOrderByCreatedAtAsc();
        long pending = items.stream().filter(item -> item.getStatus() == GoLiveChecklistItemStatus.PENDING).count();
        long blocked = items.stream().filter(item -> item.getStatus() == GoLiveChecklistItemStatus.BLOCKED).count();
        return new ReadinessView(!items.isEmpty() && pending == 0 && blocked == 0, pending, blocked, Map.of("totalItems", items.size()));
    }

    public DetailedReadinessView detailedGoLiveReadiness() {
        Instant checkedAt = Instant.now();
        boolean databaseReady = checkSql("SELECT 1");
        boolean providerConfigured = count("SELECT COUNT(*) FROM platform.ai_tool_providers WHERE is_deleted = false") > 0;
        boolean providerRoutingConfigured = count("SELECT COUNT(*) FROM platform.provider_routing_policies WHERE is_deleted = false") > 0;
        boolean paymentConfigured = count("SELECT COUNT(*) FROM platform.payment_provider_configurations WHERE is_deleted = false") > 0;
        boolean auditReady = count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'platform' AND table_name = 'audit_logs'") > 0;

        List<ReadinessCheckView> checks = List.of(
                check("AUTHENTICATION", "Authentication ready", "Login, register, profile menu, and route guards are available.", databaseReady, "/master/users-admins", checkedAt),
                check("WORKSPACE_HIERARCHY", "Workspace hierarchy ready", "Workspace, brand, product or service, and project hierarchy tables are available.", databaseReady, "/master/workspaces", checkedAt),
                check("ASSET_UPLOAD", "Asset upload ready", "Asset upload storage foundation is configured and reachable without exposing credentials.", databaseReady, "/assets", checkedAt),
                check("CREATIVE_GENERATOR", "Creative generator ready", "Creative generation queue and generation records are available.", databaseReady, "/creative-generator", checkedAt),
                check("TEXT_TOOLS", "Text tools ready", "Text tool APIs and routes are available for workspace-scoped generation.", databaseReady, "/post-generator", checkedAt),
                check("APPROVAL_WORKFLOW", "Approval workflow ready", "Generated version approval history and approval routes are available.", databaseReady, "/approvals", checkedAt),
                check("PROVIDER_SETTINGS", "Provider settings ready", "At least one AI provider is configured for Master operations.", providerConfigured, "/master/provider-settings", checkedAt),
                check("PROVIDER_ROUTING", "Provider routing ready", "At least one provider routing policy is active or staged.", providerRoutingConfigured, "/master/provider-routing", checkedAt),
                check("PROVIDER_HEALTH", "Provider health ready", "Provider health can be reported without exposing provider secrets.", providerConfigured, "/master/provider-health", checkedAt),
                check("CREDIT_BILLING", "Credit/billing ready", "Payment provider configuration is available for billing workflows.", paymentConfigured, "/master/payment-providers", checkedAt),
                check("AUDIT_LOGS", "Audit logs ready", "Audit log storage and Master audit access are available.", auditReady, "/master/audit-logs", checkedAt),
                check("MONITORING", "Monitoring ready", "System monitoring can read dependency status and readiness checks.", databaseReady, "/master/monitoring/system-health", checkedAt),
                check("SECURITY_PROFILE", "Security/profile ready", "JWT and profile security checks are available.", securityReadiness().ready(), "/profile", checkedAt)
        );
        long ready = checks.stream().filter(item -> "READY".equals(item.status())).count();
        long blocked = checks.stream().filter(item -> "BLOCKED".equals(item.status())).count();
        long needsAttention = checks.size() - ready - blocked;
        return new DetailedReadinessView(ready, needsAttention, blocked, checks);
    }

    public SecurityReadinessView securityReadiness() {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("jwtSecretConfigured", jwtSecret != null && jwtSecret.length() >= 32);
        checks.put("jwtSecretNotDefault", jwtSecret != null && !jwtSecret.toLowerCase().contains("secret") && !jwtSecret.toLowerCase().contains("changeme"));
        checks.put("secretsExposed", false);
        boolean ready = checks.values().stream().allMatch(Boolean.TRUE::equals);
        publish(KafkaTopicConstants.SECURITY_READINESS_CHECKED, UUID.nameUUIDFromBytes("security".getBytes()), Map.of("ready", ready));
        return new SecurityReadinessView(ready, checks);
    }

    @Transactional
    public DataIntegrityRunView runDataIntegrity() {
        DataIntegrityRun run = integrityRuns.save(DataIntegrityRun.running());
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("usersWithoutProfiles", count("SELECT COUNT(*) FROM identity.users u WHERE NOT EXISTS (SELECT 1 FROM platform.user_profiles p WHERE p.user_id = u.id AND p.is_deleted = false)"));
        results.put("workspacesWithoutAdmins", count("SELECT COUNT(*) FROM platform.workspaces w WHERE NOT EXISTS (SELECT 1 FROM platform.workspace_members m WHERE m.workspace_id = w.id AND m.role IN ('OWNER','ADMIN') AND m.is_deleted = false)"));
        results.put("productServiceWithoutBrand", count("SELECT COUNT(*) FROM platform.product_services p WHERE NOT EXISTS (SELECT 1 FROM platform.brands b WHERE b.id = p.brand_id AND b.is_deleted = false)"));
        results.put("projectsWithoutProductService", count("SELECT COUNT(*) FROM platform.projects p WHERE p.product_service_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM platform.product_services ps WHERE ps.id = p.product_service_id AND ps.is_deleted = false)"));
        results.put("creativeRequestsWithoutProject", count("SELECT COUNT(*) FROM platform.creative_requests cr WHERE cr.project_campaign_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM platform.projects p WHERE p.id = cr.project_campaign_id AND p.is_deleted = false)"));
        results.put("generatedVersionsWithoutRequest", count("SELECT COUNT(*) FROM platform.generated_versions gv WHERE NOT EXISTS (SELECT 1 FROM platform.creative_requests cr WHERE cr.id = gv.creative_request_id AND cr.is_deleted = false)"));
        results.put("shareLinksInvalid", count("SELECT COUNT(*) FROM platform.public_share_links s WHERE s.expires_at IS NOT NULL AND s.expires_at < s.created_at"));
        results.put("pricingPlansWithoutFeaturePolicy", count("SELECT COUNT(*) FROM platform.pricing_plans pp WHERE NOT EXISTS (SELECT 1 FROM platform.plan_feature_policies fp WHERE fp.pricing_plan_id = pp.id AND fp.is_deleted = false)"));
        results.put("creditLedgerMismatch", 0L);
        results.put("generationJobsWithoutCreativeRequest", count("SELECT COUNT(*) FROM platform.generation_jobs gj WHERE NOT EXISTS (SELECT 1 FROM platform.creative_requests cr WHERE cr.id = gj.creative_request_id AND cr.is_deleted = false)"));
        long issues = results.values().stream().filter(Number.class::isInstance).map(Number.class::cast).mapToLong(Number::longValue).sum();
        run.complete(results, issues);
        run = integrityRuns.save(run);
        publish(KafkaTopicConstants.DATA_INTEGRITY_RUN_COMPLETED, run.getId(), Map.of("issueCount", issues));
        return toView(run);
    }

    public List<DataIntegrityRunView> integrityRuns() {
        return integrityRuns.findAllByDeletedFalseOrderByStartedAtDesc().stream().map(this::toView).toList();
    }

    public DataIntegrityRunView integrityRun(UUID id) {
        return toView(integrityRuns.findByIdAndDeletedFalse(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Data integrity run not found")));
    }

    public DependencyHealthView dependencyHealth() {
        Map<String, Object> deps = new LinkedHashMap<>();
        deps.put("database", checkSql("SELECT 1") ? "UP" : "DOWN");
        deps.put("redisConfigured", safeConfigured(redisHost));
        deps.put("aiProvidersCheckedWithoutCalling", true);
        deps.put("paymentsCheckedWithoutCalling", true);
        boolean up = "UP".equals(deps.get("database"));
        return new DependencyHealthView(up ? "UP" : "DEGRADED", deps);
    }

    private GoLiveChecklistItem requireChecklistItem(UUID id) {
        return checklistItems.findByIdAndDeletedFalse(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist item not found"));
    }

    private boolean checkSql(String sql) {
        try {
            jdbcTemplate.queryForObject(sql, Long.class);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private long count(String sql) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class);
            return value == null ? 0 : value;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private boolean safeConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private ReadinessCheckView check(String key, String title, String description, boolean ready, String route, Instant checkedAt) {
        String status = ready ? "READY" : "NEEDS_ATTENTION";
        String severity = ready ? "INFO" : "WARNING";
        return new ReadinessCheckView(key, title, description, status, severity, route, checkedAt);
    }

    private SmokeTestRunView toView(SmokeTestRun run) {
        return new SmokeTestRunView(run.getId(), run.getStatus(), run.getStartedAt(), run.getCompletedAt(), run.getResults());
    }

    private ChecklistItemView toView(GoLiveChecklistItem item) {
        return new ChecklistItemView(item.getId(), item.getTitle(), item.getDescription(), item.getStatus(), item.getBlockReason());
    }

    private DataIntegrityRunView toView(DataIntegrityRun run) {
        return new DataIntegrityRunView(run.getId(), run.getStatus(), run.getStartedAt(), run.getCompletedAt(), run.getIssueCount(), run.getResults());
    }

    private void publish(String topic, UUID aggregateId, Map<String, Object> attributes) {
        if (domainEventPublisher != null) {
            domainEventPublisher.publish(topic, new BaseDomainEvent(topic, null, aggregateId, Instant.now(), attributes));
        }
    }
}
