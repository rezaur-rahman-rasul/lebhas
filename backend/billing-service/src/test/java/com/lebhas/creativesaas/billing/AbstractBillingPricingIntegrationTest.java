package com.lebhas.creativesaas.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.pricing.cache.PlanFeaturePolicyCacheService;
import com.lebhas.creativesaas.pricing.cache.PricingPlanCacheService;
import com.lebhas.creativesaas.pricing.cache.WorkspaceSubscriptionCacheService;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import com.lebhas.pricing.PlanFeaturePolicy;
import com.lebhas.pricing.PlanFeaturePolicyRepository;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.PricingPlanRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
abstract class AbstractBillingPricingIntegrationTest {

    private static final Set<String> SEED_PLAN_CODES = Set.of("FREE", "BASIC", "PRO", "ENTERPRISE");

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
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected WorkspaceRepository workspaceRepository;

    @Autowired
    protected WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
    protected PricingPlanRepository pricingPlanRepository;

    @Autowired
    protected PlanFeaturePolicyRepository planFeaturePolicyRepository;

    @Autowired
    protected WorkspaceSubscriptionRepository workspaceSubscriptionRepository;

    @Autowired
    protected JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected PricingPlanCacheService pricingPlanCacheService;

    @Autowired
    protected PlanFeaturePolicyCacheService planFeaturePolicyCacheService;

    @Autowired
    protected WorkspaceSubscriptionCacheService workspaceSubscriptionCacheService;

    protected UserEntity masterUser;
    protected UserEntity adminUser;
    protected WorkspaceEntity workspaceOne;

    @BeforeEach
    void setUpBillingPricingFoundation() {
        workspaceSubscriptionRepository.deleteAll();
        deleteCustomPlansAndPolicies();
        workspaceMembershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();

        masterUser = userRepository.save(UserEntity.register(
                "Master",
                "User",
                "pricing-master@example.com",
                null,
                "{noop}unused",
                Role.MASTER,
                UserStatus.ACTIVE,
                true));
        adminUser = userRepository.save(UserEntity.register(
                "Workspace",
                "Admin",
                "pricing-admin@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));

        workspaceOne = workspaceRepository.save(WorkspaceEntity.create(
                "Pricing Workspace",
                "pricing-workspace",
                null,
                null,
                "Retail",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "USD",
                "US",
                adminUser.getId()));
        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspaceOne.getId(),
                adminUser.getId(),
                Role.ADMIN,
                WorkspaceMembershipStatus.ACTIVE,
                Set.of(),
                Instant.now(),
                adminUser.getId()));
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected String masterToken() {
        return jwtAccessTokenService.generate(masterUser, null, Role.MASTER).token();
    }

    protected String adminToken() {
        return jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();
    }

    protected PricingPlan createCustomPlan(String codePrefix, boolean active) {
        String code = (codePrefix + "-" + UUID.randomUUID()).toUpperCase().replace('-', '_');
        PricingPlan pricingPlan = PricingPlan.create(
                "Plan " + codePrefix,
                code,
                "Test pricing plan " + codePrefix,
                new BigDecimal("12.5000"),
                new BigDecimal("125.0000"),
                "USD",
                false,
                active,
                90);
        return pricingPlanRepository.save(pricingPlan);
    }

    protected PlanFeaturePolicy createFeaturePolicy(
            UUID pricingPlanId,
            Integer maxGeneratedVersionsPerRequest,
            BigDecimal maxStorageGb,
            boolean allowPublicShareLinks
    ) {
        PlanFeaturePolicy policy = PlanFeaturePolicy.create(
                pricingPlanId,
                maxGeneratedVersionsPerRequest,
                5,
                10,
                12,
                8,
                maxStorageGb,
                new BigDecimal("200.0000"),
                true,
                allowPublicShareLinks,
                false,
                false,
                true,
                true);
        return planFeaturePolicyRepository.save(policy);
    }

    protected WorkspaceSubscription createSubscription(UUID workspaceId, UUID pricingPlanId, WorkspaceSubscriptionStatus status) {
        WorkspaceSubscription subscription = WorkspaceSubscription.create(
                workspaceId,
                pricingPlanId,
                status,
                Instant.now(),
                Instant.now().plusSeconds(86400),
                null,
                true);
        return workspaceSubscriptionRepository.save(subscription);
    }

    protected PricingPlan seedPlan(String code) {
        return pricingPlanRepository.findByCodeIgnoreCaseAndDeletedFalse(code).orElseThrow();
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected UUID uuidAt(MvcResult result, String pointer) throws Exception {
        return UUID.fromString(json(result).at(pointer).asText());
    }

    private void deleteCustomPlansAndPolicies() {
        Set<UUID> customPlanIds = pricingPlanRepository.findAll().stream()
                .filter(plan -> !SEED_PLAN_CODES.contains(plan.getCode()))
                .map(PricingPlan::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (customPlanIds.isEmpty()) {
            return;
        }
        planFeaturePolicyRepository.findAll().stream()
                .filter(policy -> customPlanIds.contains(policy.getPricingPlanId()))
                .forEach(planFeaturePolicyRepository::delete);
        pricingPlanRepository.findAllById(customPlanIds)
                .forEach(pricingPlanRepository::delete);
    }
}
