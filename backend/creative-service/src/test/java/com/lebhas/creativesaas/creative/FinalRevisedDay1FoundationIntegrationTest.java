package com.lebhas.creativesaas.creative;

import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import com.lebhas.creativesaas.credit.application.CreditWalletService;
import com.lebhas.creativesaas.credit.domain.CreditTransactionEntity;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditTransactionRepository;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditWalletRepository;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.download.domain.DownloadLogEntity;
import com.lebhas.creativesaas.download.infrastructure.persistence.DownloadLogRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.sharing.domain.PublicShareLinkEntity;
import com.lebhas.creativesaas.sharing.infrastructure.persistence.PublicShareLinkRepository;
import com.lebhas.creativesaas.usage.domain.UsageBillingLogEntity;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FinalRevisedDay1FoundationIntegrationTest {

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
    private BrandRepository brandRepository;

    @Autowired
    private ProductServiceRepository productServiceRepository;

    @Autowired
    private ProjectCampaignRepository projectCampaignRepository;

    @Autowired
    private CreativeRequestRepository creativeRequestRepository;

    @Autowired
    private GeneratedVersionRepository generatedVersionRepository;

    @Autowired
    private PublicShareLinkRepository publicShareLinkRepository;

    @Autowired
    private DownloadLogRepository downloadLogRepository;

    @Autowired
    private UsageBillingLogRepository usageBillingLogRepository;

    @Autowired
    private CreditWalletRepository creditWalletRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private CreditWalletService creditWalletService;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UserEntity adminUser;
    private UserEntity crewUser;
    private UserEntity masterUser;
    private UserEntity workspaceTwoUser;
    private WorkspaceEntity workspaceOne;
    private WorkspaceEntity workspaceTwo;
    private BrandEntity brandOne;
    private ProductServiceEntity productServiceOne;
    private ProjectCampaignEntity projectCampaignOne;
    private CreativeRequestEntity creativeRequestOne;
    private GeneratedVersionEntity generatedVersionOne;

    @BeforeEach
    void setUp() {
        usageBillingLogRepository.deleteAll();
        downloadLogRepository.deleteAll();
        publicShareLinkRepository.deleteAll();
        generatedVersionRepository.deleteAll();
        creativeRequestRepository.deleteAll();
        projectCampaignRepository.deleteAll();
        productServiceRepository.deleteAll();
        creditTransactionRepository.deleteAll();
        creditWalletRepository.deleteAll();
        brandRepository.deleteAll();
        workspaceMembershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        TenantContext.clear();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        adminUser = userRepository.save(UserEntity.register(
                "Creative",
                "Admin",
                "creative-admin@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));
        crewUser = userRepository.save(UserEntity.register(
                "Creative",
                "Crew",
                "creative-crew@example.com",
                null,
                "{noop}unused",
                Role.CREW,
                UserStatus.ACTIVE,
                true));
        masterUser = userRepository.save(UserEntity.register(
                "Master",
                "Support",
                "creative-master@example.com",
                null,
                "{noop}unused",
                Role.MASTER,
                UserStatus.ACTIVE,
                true));
        workspaceTwoUser = userRepository.save(UserEntity.register(
                "Other",
                "Admin",
                "creative-other@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));

        workspaceOne = workspaceRepository.save(WorkspaceEntity.create(
                "Creative Workspace One",
                "creative-workspace-one",
                null,
                null,
                "Media",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                adminUser.getId()));
        workspaceTwo = workspaceRepository.save(WorkspaceEntity.create(
                "Creative Workspace Two",
                "creative-workspace-two",
                null,
                null,
                "Agency",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                workspaceTwoUser.getId()));

        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspaceOne.getId(),
                adminUser.getId(),
                Role.ADMIN,
                WorkspaceMembershipStatus.ACTIVE,
                java.util.Set.of(),
                Instant.now(),
                adminUser.getId()));
        WorkspaceMembershipEntity crewMembership = WorkspaceMembershipEntity.create(
                workspaceOne.getId(),
                crewUser.getId(),
                Role.CREW,
                WorkspaceMembershipStatus.ACTIVE,
                java.util.Set.of(),
                false,
                false,
                Instant.now(),
                adminUser.getId());
        workspaceMembershipRepository.save(crewMembership);

        brandOne = brandRepository.save(BrandEntity.create(
                workspaceOne.getId(),
                adminUser.getId(),
                "Brand One",
                "Agency",
                "Creative",
                "Growth teams",
                "Bold",
                "Start now",
                "#123456",
                "#654321",
                "https://brand-one.example.com",
                null,
                null,
                null,
                null,
                BrandLanguagePreference.BOTH));
        BrandEntity brandTwo = brandRepository.save(BrandEntity.create(
                workspaceTwo.getId(),
                workspaceTwoUser.getId(),
                "Brand Two",
                "Service",
                "Consulting",
                "Finance teams",
                "Calm",
                "Learn more",
                "#111111",
                "#222222",
                "https://brand-two.example.com",
                null,
                null,
                null,
                null,
                BrandLanguagePreference.BOTH));

        productServiceOne = productServiceRepository.save(ProductServiceEntity.create(
                workspaceOne.getId(),
                brandOne.getId(),
                "Creative Retainer",
                "Monthly delivery",
                "SERVICE",
                "Growth teams",
                "Fast delivery"));
        ProductServiceEntity productServiceTwo = productServiceRepository.save(ProductServiceEntity.create(
                workspaceTwo.getId(),
                brandTwo.getId(),
                "Consulting Package",
                "Campaign consulting",
                "SERVICE",
                "Finance teams",
                "Operational clarity"));

        projectCampaignOne = projectCampaignRepository.save(ProjectCampaignEntity.create(
                workspaceOne.getId(),
                brandOne.getId(),
                productServiceOne.getId(),
                adminUser.getId(),
                "Spring Launch",
                "Launch campaign foundation",
                "AWARENESS",
                "FACEBOOK",
                "CAMPAIGN"));
        ProjectCampaignEntity projectCampaignTwo = projectCampaignRepository.save(ProjectCampaignEntity.create(
                workspaceTwo.getId(),
                brandTwo.getId(),
                productServiceTwo.getId(),
                workspaceTwoUser.getId(),
                "Summer Launch",
                "Cross-workspace boundary",
                "LEADS",
                "INSTAGRAM",
                "CAMPAIGN"));

        creativeRequestOne = creativeRequestRepository.save(CreativeRequestEntity.create(
                workspaceOne.getId(),
                projectCampaignOne.getId(),
                crewUser.getId(),
                "Launch Creative",
                "Create a product launch creative",
                "Enhanced launch creative prompt",
                "AWARENESS",
                "FACEBOOK",
                "PNG",
                java.util.List.of(),
                null));
        CreativeRequestEntity creativeRequestTwo = creativeRequestRepository.save(CreativeRequestEntity.create(
                workspaceTwo.getId(),
                projectCampaignTwo.getId(),
                workspaceTwoUser.getId(),
                "Other Creative",
                "Create a consulting visual",
                null,
                "LEADS",
                "INSTAGRAM",
                "PNG",
                java.util.List.of(),
                null));

        generatedVersionOne = generatedVersionRepository.save(GeneratedVersionEntity.create(
                workspaceOne.getId(),
                creativeRequestOne.getId(),
                projectCampaignOne.getId(),
                1,
                "Version 1",
                null,
                null,
                "OPENAI",
                "gpt-image-1.5",
                adminUser.getId()));
        GeneratedVersionEntity generatedVersionTwo = generatedVersionRepository.save(GeneratedVersionEntity.create(
                workspaceTwo.getId(),
                creativeRequestTwo.getId(),
                projectCampaignTwo.getId(),
                1,
                "Version 1",
                null,
                null,
                "OPENAI",
                "gpt-image-1.5",
                workspaceTwoUser.getId()));

        publicShareLinkRepository.save(PublicShareLinkEntity.create(
                workspaceOne.getId(),
                generatedVersionOne.getId(),
                "public-token-1",
                Instant.now().plusSeconds(3600)));
        downloadLogRepository.save(DownloadLogEntity.create(
                workspaceOne.getId(),
                generatedVersionOne.getId(),
                adminUser.getId(),
                "PUBLIC",
                "127.0.0.1",
                "JUnit"));

        CreditWalletEntity wallet = creditWalletService.initializeWallet(workspaceOne.getId());
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo("0.0000");
        CreditTransactionEntity purchase = creditWalletService.purchase(
                workspaceOne.getId(),
                new BigDecimal("100.00"),
                "WORKSPACE_TOPUP",
                workspaceOne.getId());
        CreditTransactionEntity reserved = creditWalletService.reserve(
                workspaceOne.getId(),
                new BigDecimal("20.00"),
                "CREATIVE_REQUEST",
                creativeRequestOne.getId());
        CreditTransactionEntity finalized = creditWalletService.finalizeReservation(
                workspaceOne.getId(),
                new BigDecimal("15.00"),
                "CREATIVE_REQUEST",
                creativeRequestOne.getId());
        CreditTransactionEntity refunded = creditWalletService.refund(
                workspaceOne.getId(),
                new BigDecimal("5.00"),
                "CREATIVE_REQUEST",
                creativeRequestOne.getId());
        usageBillingLogRepository.save(UsageBillingLogEntity.create(
                workspaceOne.getId(),
                generatedVersionOne.getId(),
                "GENERATION",
                finalized.getId(),
                new BigDecimal("15.00"),
                "COMPLETED"));

        assertThat(purchase.getId()).isNotNull();
        assertThat(reserved.getId()).isNotNull();
        assertThat(finalized.getId()).isNotNull();
        assertThat(refunded.getId()).isNotNull();
        assertThat(generatedVersionTwo.getWorkspaceId()).isEqualTo(workspaceTwo.getId());
    }

    @Test
    void shouldPersistFinalDay1HierarchyAndCreditLifecycle() {
        assertThat(productServiceRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceOne.getId()))
                .singleElement()
                .extracting(ProductServiceEntity::getBrandId)
                .isEqualTo(brandOne.getId());
        assertThat(projectCampaignRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceOne.getId()))
                .singleElement()
                .extracting(ProjectCampaignEntity::getProductServiceId)
                .isEqualTo(productServiceOne.getId());
        assertThat(creativeRequestRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceOne.getId()))
                .singleElement()
                .extracting(CreativeRequestEntity::getProjectCampaignId)
                .isEqualTo(projectCampaignOne.getId());
        assertThat(generatedVersionRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceOne.getId()))
                .singleElement()
                .extracting(GeneratedVersionEntity::getCreativeRequestId)
                .isEqualTo(creativeRequestOne.getId());
        assertThat(publicShareLinkRepository.findFirstByGeneratedVersionIdAndDeletedFalse(generatedVersionOne.getId())).isPresent();
        assertThat(downloadLogRepository.findAllByGeneratedVersionIdAndDeletedFalse(generatedVersionOne.getId())).hasSize(1);
        assertThat(usageBillingLogRepository.findAllByGeneratedVersionIdAndDeletedFalse(generatedVersionOne.getId())).hasSize(1);

        CreditWalletEntity wallet = creditWalletRepository.findByWorkspaceIdAndDeletedFalse(workspaceOne.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("85.0000");
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo("0.0000");
        assertThat(creditTransactionRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceOne.getId())).hasSize(4);

        WorkspaceMembershipEntity crewMembership = workspaceMembershipRepository.findByUserIdAndWorkspaceIdAndDeletedFalse(crewUser.getId(), workspaceOne.getId()).orElseThrow();
        assertThat(crewMembership.canDownloadCreative()).isFalse();
        assertThat(crewMembership.canEditCreative()).isFalse();
        crewMembership.updateCreativeAccess(true, true);
        workspaceMembershipRepository.save(crewMembership);
        assertThat(crewMembership.canDownloadCreative()).isTrue();
        assertThat(crewMembership.canEditCreative()).isTrue();

        TenantContext.setWorkspaceId(workspaceOne.getId());
        try {
            assertThat(generatedVersionRepository.findAllForCurrentTenant())
                    .extracting(GeneratedVersionEntity::getId)
                    .containsExactly(generatedVersionOne.getId());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void shouldExposeWorkspaceScopedFoundationApisAndEnforceIsolation() {
        String adminToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();
        String masterToken = jwtAccessTokenService.generate(masterUser, null, Role.MASTER).token();

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/projects", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(1))
                .body("data[0].id", equalTo(projectCampaignOne.getId().toString()));

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/creative-requests", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(1))
                .body("data[0].id", equalTo(creativeRequestOne.getId().toString()));

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/generated-versions", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(1))
                .body("data[0].id", equalTo(generatedVersionOne.getId().toString()));

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/projects", workspaceTwo.getId())
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("TENANT-403"));

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/creative-requests", workspaceTwo.getId())
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("TENANT-403"));

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/generated-versions", workspaceTwo.getId())
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("TENANT-403"));

        given()
                .header("Authorization", "Bearer " + masterToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/generated-versions", workspaceTwo.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }
}
