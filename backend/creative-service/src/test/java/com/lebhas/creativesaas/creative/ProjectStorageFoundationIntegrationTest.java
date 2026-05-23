package com.lebhas.creativesaas.creative;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.storage.application.StoragePathBuilder;
import com.lebhas.creativesaas.storage.domain.StorageClass;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.storage.domain.StorageFilePurpose;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageFileRepository;
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

import java.sql.Blob;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProjectStorageFoundationIntegrationTest {

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
    private StorageFileRepository storageFileRepository;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private StoragePathBuilder storagePathBuilder;

    private UserEntity adminUser;
    private WorkspaceEntity workspace;
    private BrandEntity brand;
    private ProductServiceEntity productService;
    private ProjectCampaignEntity projectCampaign;
    private StorageFileEntity storageFile;

    @BeforeEach
    void setUp() {
        storageFileRepository.deleteAll();
        projectCampaignRepository.deleteAll();
        productServiceRepository.deleteAll();
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
                "storage-foundation@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));

        workspace = workspaceRepository.save(WorkspaceEntity.create(
                "Creative Workspace",
                "creative-workspace-foundation",
                null,
                null,
                "Media",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                adminUser.getId()));
        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspace.getId(),
                adminUser.getId(),
                Role.ADMIN,
                WorkspaceMembershipStatus.ACTIVE,
                java.util.Set.of(),
                Instant.now(),
                adminUser.getId()));

        brand = brandRepository.save(BrandEntity.create(
                workspace.getId(),
                adminUser.getId(),
                "Generation Brand",
                "Agency",
                "Creative",
                "Growth teams",
                "Confident",
                "Start now",
                "#123456",
                "#654321",
                "https://generation.example.com",
                null,
                null,
                null,
                null,
                BrandLanguagePreference.BOTH));
        productService = productServiceRepository.save(ProductServiceEntity.create(
                workspace.getId(),
                brand.getId(),
                "Creative Retainer",
                "Monthly delivery",
                "SERVICE",
                "Growth teams",
                "Fast delivery"));
        projectCampaign = projectCampaignRepository.save(ProjectCampaignEntity.create(
                workspace.getId(),
                brand.getId(),
                productService.getId(),
                adminUser.getId(),
                "Spring Launch",
                "Workspace default creative project",
                "AWARENESS",
                "FACEBOOK",
                "CAMPAIGN"));

        storageFile = storageFileRepository.save(StorageFileEntity.create(
                workspace.getId(),
                projectCampaign.getId(),
                StorageProvider.R2,
                "creative-r2-assets",
                storagePathBuilder.buildGeneratedPath(workspace.getId(), projectCampaign.getId(), UUID.randomUUID()),
                "https://cdn.example.com/generated/file-1",
                "image/png",
                "png",
                1024L,
                "abc123hash",
                1080,
                1080,
                null,
                StorageClass.STANDARD,
                StorageFilePurpose.GENERATED));
    }

    @Test
    void shouldListWorkspaceProjects() {
        String accessToken = tokenFor(adminUser, workspace.getId(), Role.ADMIN);

        given()
                .header("Authorization", bearer(accessToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/projects", workspace.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(1))
                .body("data[0].id", equalTo(projectCampaign.getId().toString()))
                .body("data[0].brandId", equalTo(brand.getId().toString()))
                .body("data[0].productServiceId", equalTo(productService.getId().toString()));
    }

    @Test
    void shouldGenerateStructuredStoragePaths() {
        assertThat(storagePathBuilder.buildRawPath(workspace.getId(), projectCampaign.getId(), storageFile.getId()))
                .isEqualTo("raw/workspaces/%s/projects/%s/%s".formatted(
                        workspace.getId(),
                        projectCampaign.getId(),
                        storageFile.getId()));
        assertThat(storagePathBuilder.buildProcessedPath(workspace.getId(), projectCampaign.getId(), storageFile.getId()))
                .isEqualTo("processed/workspaces/%s/projects/%s/%s".formatted(
                        workspace.getId(),
                        projectCampaign.getId(),
                        storageFile.getId()));
        assertThat(storagePathBuilder.buildGeneratedPath(workspace.getId(), projectCampaign.getId(), storageFile.getId()))
                .isEqualTo("generated/workspaces/%s/projects/%s/%s".formatted(
                        workspace.getId(),
                        projectCampaign.getId(),
                        storageFile.getId()));
        assertThat(storagePathBuilder.buildThumbnailPath(workspace.getId(), projectCampaign.getId(), storageFile.getId()))
                .isEqualTo("thumbnails/workspaces/%s/projects/%s/%s".formatted(
                        workspace.getId(),
                        projectCampaign.getId(),
                        storageFile.getId()));
    }

    @Test
    void shouldStoreStorageMetadataOnlyAndExposeItViaApi() {
        assertThat(Arrays.stream(StorageFileEntity.class.getDeclaredFields()))
                .noneMatch(field -> field.getType().equals(byte[].class)
                        || field.getType().equals(Blob.class)
                        || java.io.InputStream.class.isAssignableFrom(field.getType()));

        String accessToken = tokenFor(adminUser, workspace.getId(), Role.ADMIN);

        given()
                .header("Authorization", bearer(accessToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/storage-files/{fileId}", workspace.getId(), storageFile.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(storageFile.getId().toString()))
                .body("data.provider", equalTo("R2"))
                .body("data.objectKey", equalTo(storageFile.getObjectKey()))
                .body("data.hash", equalTo(storageFile.getHash()));
    }

    @Test
    void shouldDetectDuplicateFileHashWithinWorkspace() {
        storageFileRepository.save(StorageFileEntity.create(
                workspace.getId(),
                projectCampaign.getId(),
                StorageProvider.R2,
                "creative-r2-assets",
                storagePathBuilder.buildRawPath(workspace.getId(), projectCampaign.getId(), UUID.randomUUID()),
                null,
                "image/png",
                "png",
                2048L,
                "abc123hash",
                1080,
                1080,
                null,
                StorageClass.STANDARD,
                StorageFilePurpose.RAW));

        assertThat(storageFileRepository.findFirstByWorkspaceIdAndHashAndDeletedFalse(workspace.getId(), "abc123hash"))
                .isPresent()
                .get()
                .extracting(StorageFileEntity::getWorkspaceId, StorageFileEntity::getHash)
                .containsExactly(workspace.getId(), "abc123hash");
    }

    @Test
    void shouldReturnStructuredNotFoundForMissingStorageFile() {
        String accessToken = tokenFor(adminUser, workspace.getId(), Role.ADMIN);

        given()
                .header("Authorization", bearer(accessToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/storage-files/{fileId}", workspace.getId(), UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("STORAGE-404-01"));
    }

    @Test
    void shouldExposeDay3AssetRoutesInOpenApi() {
        given()
                .when()
                .get("/v3/api-docs")
                .then()
                .statusCode(200)
                .body(containsString("/api/v1/workspaces/{workspaceId}/projects/{projectId}/assets/upload"))
                .body(containsString("/api/v1/workspaces/{workspaceId}/projects/{projectId}/assets"))
                .body(containsString("/api/v1/workspaces/{workspaceId}/assets/{assetId}/preview-url"))
                .body(containsString("/api/v1/workspaces/{workspaceId}/assets/{assetId}/download-url"));
    }

    private String tokenFor(UserEntity user, UUID workspaceId, Role role) {
        return jwtAccessTokenService.generate(user, workspaceId, role).token();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
