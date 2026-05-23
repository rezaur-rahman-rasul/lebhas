package com.lebhas.creativesaas.workspace;

import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import com.lebhas.creativesaas.project.infrastructure.persistence.ProjectRepository;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.PricingPlanRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RevisedFoundationWorkspaceIntegrationTest {

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
    private ProjectRepository projectRepository;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Autowired
    private WorkspaceSubscriptionRepository workspaceSubscriptionRepository;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UserEntity adminUser;
    private UserEntity outsiderUser;
    private WorkspaceEntity workspaceOne;
    private WorkspaceEntity workspaceTwo;
    private BrandEntity primaryBrand;
    private ProjectEntity primaryProject;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        brandRepository.deleteAll();
        workspaceSubscriptionRepository.deleteAll();
        workspaceMembershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        TenantContext.clear();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        adminUser = userRepository.save(UserEntity.register(
                "Admin",
                "User",
                "admin.foundation@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));
        outsiderUser = userRepository.save(UserEntity.register(
                "Other",
                "User",
                "other.foundation@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));

        workspaceOne = workspaceRepository.save(WorkspaceEntity.create(
                "Workspace One",
                "workspace-one-foundation",
                null,
                null,
                "Retail",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                adminUser.getId()));
        workspaceTwo = workspaceRepository.save(WorkspaceEntity.create(
                "Workspace Two",
                "workspace-two-foundation",
                null,
                null,
                "Services",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                outsiderUser.getId()));

        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspaceOne.getId(),
                adminUser.getId(),
                Role.ADMIN,
                WorkspaceMembershipStatus.ACTIVE,
                java.util.Set.of(),
                Instant.now(),
                adminUser.getId()));

        primaryBrand = brandRepository.save(BrandEntity.create(
                workspaceOne.getId(),
                adminUser.getId(),
                "Lebhas Main Brand",
                "Agency",
                "Creative",
                "SMBs",
                "Direct",
                "Book now",
                "#112233",
                "#445566",
                "https://lebhas.example.com",
                "https://facebook.com/lebhas",
                "https://instagram.com/lebhas",
                "https://linkedin.com/company/lebhas",
                "https://tiktok.com/@lebhas",
                BrandLanguagePreference.BOTH));
        primaryProject = projectRepository.save(ProjectEntity.create(
                workspaceOne.getId(),
                primaryBrand.getId(),
                "Launch Campaign",
                "Default project for revised day 1",
                null,
                null));
        brandRepository.save(BrandEntity.create(
                workspaceTwo.getId(),
                outsiderUser.getId(),
                "External Brand",
                "Service",
                "Consulting",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BrandLanguagePreference.BOTH));
    }

    @Test
    void shouldPersistUserBrandAndBrandProjectRelationships() {
        BrandEntity storedBrand = brandRepository.findById(primaryBrand.getId()).orElseThrow();
        ProjectEntity storedProject = projectRepository.findById(primaryProject.getId()).orElseThrow();

        assertThat(storedBrand.getOwnerUserId()).isEqualTo(adminUser.getId());
        assertThat(storedProject.getBrandId()).isEqualTo(storedBrand.getId());
        assertThat(storedProject.getWorkspaceId()).isEqualTo(storedBrand.getWorkspaceId());
    }

    @Test
    void shouldRequireBrandOwnerAndProjectBrand() {
        assertThatThrownBy(() -> BrandEntity.create(
                workspaceOne.getId(),
                null,
                "Invalid Brand",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BrandLanguagePreference.BOTH))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ProjectEntity.create(
                workspaceOne.getId(),
                null,
                "Invalid Project",
                null,
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFilterBrandsByCurrentTenantContext() {
        TenantContext.setWorkspaceId(workspaceOne.getId());
        try {
            assertThat(brandRepository.findAllForCurrentTenant())
                    .extracting(BrandEntity::getId)
                    .containsExactly(primaryBrand.getId());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void shouldListWorkspaceBrandsWithApiResponseEnvelope() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/brands", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(1))
                .body("data[0].id", equalTo(primaryBrand.getId().toString()))
                .body("data[0].ownerUserId", equalTo(adminUser.getId().toString()))
                .body("data[0].languagePreference", equalTo("BOTH"));
    }

    @Test
    void shouldPreventBrandListingAcrossWorkspaces() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/brands", workspaceTwo.getId())
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("TENANT-403"));
    }

    @Test
    void shouldCreateBrandWithLanguagePreference() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                        {
                          "name": "Bangla Brand",
                          "businessType": "Agency",
                          "industry": "Fashion",
                          "targetAudience": "Youth",
                          "brandVoice": "Warm",
                          "preferredCta": "Order now",
                          "primaryColor": "#123456",
                          "secondaryColor": "#654321",
                          "website": "https://brand.example.com",
                          "facebookUrl": null,
                          "instagramUrl": null,
                          "linkedinUrl": null,
                          "tiktokUrl": null,
                          "languagePreference": "BANGLA"
                        }
                        """)
                .when()
                .post("/api/v1/workspaces/{workspaceId}/brands", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.languagePreference", equalTo("BANGLA"));
    }

    @Test
    void shouldUpdateBrandLanguagePreference() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                        {
                          "name": "Lebhas Main Brand",
                          "businessType": "Agency",
                          "industry": "Creative",
                          "targetAudience": "SMBs",
                          "brandVoice": "Direct",
                          "preferredCta": "Book now",
                          "primaryColor": "#112233",
                          "secondaryColor": "#445566",
                          "website": "https://lebhas.example.com",
                          "facebookUrl": "https://facebook.com/lebhas",
                          "instagramUrl": "https://instagram.com/lebhas",
                          "linkedinUrl": "https://linkedin.com/company/lebhas",
                          "tiktokUrl": "https://tiktok.com/@lebhas",
                          "languagePreference": "ENGLISH",
                          "status": "ACTIVE"
                        }
                        """)
                .when()
                .put("/api/v1/workspaces/{workspaceId}/brands/{brandId}", workspaceOne.getId(), primaryBrand.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.languagePreference", equalTo("ENGLISH"));

        assertThat(brandRepository.findById(primaryBrand.getId()).orElseThrow().getLanguagePreference())
                .isEqualTo(BrandLanguagePreference.ENGLISH);
    }

    @Test
    void shouldRejectMissingBrandLanguagePreference() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                        {
                          "name": "Invalid Brand"
                        }
                        """)
                .when()
                .post("/api/v1/workspaces/{workspaceId}/brands", workspaceOne.getId())
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors[0].field", equalTo("languagePreference"));
    }

    @Test
    void shouldCreateBrandWithEnglishLanguagePreference() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                        {
                          "name": "English Brand",
                          "businessType": "Agency",
                          "industry": "Fashion",
                          "targetAudience": "Adults",
                          "brandVoice": "Confident",
                          "preferredCta": "Shop today",
                          "primaryColor": "#111111",
                          "secondaryColor": "#222222",
                          "website": "https://english-brand.example.com",
                          "facebookUrl": null,
                          "instagramUrl": null,
                          "linkedinUrl": null,
                          "tiktokUrl": null,
                          "languagePreference": "ENGLISH"
                        }
                        """)
                .when()
                .post("/api/v1/workspaces/{workspaceId}/brands", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.languagePreference", equalTo("ENGLISH"));
    }

    @Test
    void shouldCreateBrandWithBothLanguagePreference() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                        {
                          "name": "Bilingual Brand",
                          "businessType": "Agency",
                          "industry": "Lifestyle",
                          "targetAudience": "Families",
                          "brandVoice": "Friendly",
                          "preferredCta": "Explore now",
                          "primaryColor": "#333333",
                          "secondaryColor": "#444444",
                          "website": "https://bilingual-brand.example.com",
                          "facebookUrl": null,
                          "instagramUrl": null,
                          "linkedinUrl": null,
                          "tiktokUrl": null,
                          "languagePreference": "BOTH"
                        }
                        """)
                .when()
                .post("/api/v1/workspaces/{workspaceId}/brands", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.languagePreference", equalTo("BOTH"));
    }

    @Test
    void shouldRejectInvalidBrandLanguagePreference() {
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                        {
                          "name": "Invalid Language Brand",
                          "businessType": "Agency",
                          "industry": "Retail",
                          "targetAudience": "Everyone",
                          "brandVoice": "Bright",
                          "preferredCta": "Buy now",
                          "primaryColor": "#101010",
                          "secondaryColor": "#202020",
                          "website": "https://invalid-language.example.com",
                          "facebookUrl": null,
                          "instagramUrl": null,
                          "linkedinUrl": null,
                          "tiktokUrl": null,
                          "languagePreference": "SPANISH"
                        }
                        """)
                .when()
                .post("/api/v1/workspaces/{workspaceId}/brands", workspaceOne.getId())
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("COMMON-400"));
    }

    @Test
    void shouldIncludeActiveSubscriptionInWorkspaceContext() {
        PricingPlan basicPlan = pricingPlanRepository.findByCodeIgnoreCaseAndDeletedFalse("BASIC").orElseThrow();
        workspaceSubscriptionRepository.save(WorkspaceSubscription.create(
                workspaceOne.getId(),
                basicPlan.getId(),
                WorkspaceSubscriptionStatus.ACTIVE,
                Instant.parse("2026-05-18T00:00:00Z"),
                Instant.parse("2026-06-18T00:00:00Z"),
                null,
                true));
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/context", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.activePricingPlan.code", equalTo("BASIC"))
                .body("data.activeSubscription.workspaceId", equalTo(workspaceOne.getId().toString()))
                .body("data.activeSubscription.status", equalTo("ACTIVE"))
                .body("data.activeSubscription.pricingPlanId", equalTo(basicPlan.getId().toString()));
    }

    @Test
    void shouldIncludeFeaturePolicyInWorkspaceContext() {
        PricingPlan basicPlan = pricingPlanRepository.findByCodeIgnoreCaseAndDeletedFalse("BASIC").orElseThrow();
        workspaceSubscriptionRepository.save(WorkspaceSubscription.create(
                workspaceOne.getId(),
                basicPlan.getId(),
                WorkspaceSubscriptionStatus.ACTIVE,
                Instant.parse("2026-05-18T00:00:00Z"),
                Instant.parse("2026-06-18T00:00:00Z"),
                null,
                true));
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/context", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.planFeaturePolicy.maxGeneratedVersionsPerRequest", equalTo(8))
                .body("data.generatedVersionLimit", equalTo(8))
                .body("data.storageLimitGb", equalTo(25.0f))
                .body("data.approvalWorkflowAvailable", equalTo(true))
                .body("data.publicShareAvailability", equalTo(true))
                .body("data.teamMemberLimit", equalTo(5))
                .body("data.creditLimit", equalTo(150.0f));
    }

    @Test
    void shouldIgnoreInactiveSubscriptionInWorkspaceContext() {
        PricingPlan basicPlan = pricingPlanRepository.findByCodeIgnoreCaseAndDeletedFalse("BASIC").orElseThrow();
        workspaceSubscriptionRepository.save(WorkspaceSubscription.create(
                workspaceOne.getId(),
                basicPlan.getId(),
                WorkspaceSubscriptionStatus.CANCELLED,
                Instant.parse("2026-05-18T00:00:00Z"),
                Instant.parse("2026-06-18T00:00:00Z"),
                null,
                false));
        String accessToken = jwtAccessTokenService.generate(adminUser, workspaceOne.getId(), Role.ADMIN).token();

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/workspaces/{workspaceId}/context", workspaceOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.activeSubscription", equalTo(null))
                .body("data.activePricingPlan.code", equalTo("FREE"));
    }
}
