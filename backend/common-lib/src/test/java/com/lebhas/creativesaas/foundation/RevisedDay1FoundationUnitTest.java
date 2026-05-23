package com.lebhas.creativesaas.foundation;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.authorization.RolePermissionRegistry;
import com.lebhas.creativesaas.credit.domain.CreditTransactionEntity;
import com.lebhas.creativesaas.credit.domain.CreditTransactionStatus;
import com.lebhas.creativesaas.credit.domain.CreditTransactionType;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import com.lebhas.creativesaas.storage.application.StoragePathBuilder;
import com.lebhas.creativesaas.storage.domain.StorageClass;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.storage.domain.StorageFilePurpose;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevisedDay1FoundationUnitTest {

    @Test
    void shouldCreateBrandOwnedByUserAndProjectBoundToBrand() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();

        BrandEntity brand = BrandEntity.create(
                workspaceId,
                ownerUserId,
                "Lebhas Brand",
                "Agency",
                "Creative",
                "SMBs",
                "Direct",
                "Book now",
                "#112233",
                "#445566",
                "https://lebhas.example.com",
                null,
                null,
                null,
                null,
                BrandLanguagePreference.ENGLISH);
        ProjectEntity project = ProjectEntity.create(
                workspaceId,
                brandId,
                "Launch Project",
                "Project foundation",
                null,
                null);

        assertThat(brand.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(brand.getOwnerUserId()).isEqualTo(ownerUserId);
        assertThat(brand.getLanguagePreference()).isEqualTo(BrandLanguagePreference.ENGLISH);
        assertThat(project.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(project.getBrandId()).isEqualTo(brandId);
    }

    @Test
    void shouldRejectMissingBrandOwnerAndProjectBrand() {
        UUID workspaceId = UUID.randomUUID();

        assertThatThrownBy(() -> BrandEntity.create(
                workspaceId,
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ownerUserId must not be null");

        assertThatThrownBy(() -> ProjectEntity.create(
                workspaceId,
                null,
                "Invalid Project",
                null,
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("brandId must not be null");
    }

    @Test
    void shouldBuildProjectScopedStoragePaths() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        StoragePathBuilder builder = new StoragePathBuilder();

        assertThat(builder.buildRawPath(workspaceId, projectId, fileId))
                .isEqualTo("raw/workspaces/%s/projects/%s/%s".formatted(workspaceId, projectId, fileId));
        assertThat(builder.buildProcessedPath(workspaceId, projectId, fileId))
                .isEqualTo("processed/workspaces/%s/projects/%s/%s".formatted(workspaceId, projectId, fileId));
        assertThat(builder.buildGeneratedPath(workspaceId, projectId, fileId))
                .isEqualTo("generated/workspaces/%s/projects/%s/%s".formatted(workspaceId, projectId, fileId));
        assertThat(builder.buildThumbnailPath(workspaceId, projectId, fileId))
                .isEqualTo("thumbnails/workspaces/%s/projects/%s/%s".formatted(workspaceId, projectId, fileId));
    }

    @Test
    void shouldKeepStorageFileEntityMetadataOnly() {
        assertThat(Arrays.stream(StorageFileEntity.class.getDeclaredFields()))
                .noneMatch(field -> field.getType().equals(byte[].class)
                        || field.getType().equals(Blob.class)
                        || InputStream.class.isAssignableFrom(field.getType()));

        StorageFileEntity file = StorageFileEntity.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                StorageProvider.S3,
                "creative-saas-assets",
                "generated/workspaces/w/projects/p/file",
                "https://cdn.example.com/generated/file",
                "image/png",
                "png",
                1024L,
                "ABC123HASH",
                1080,
                1080,
                null,
                StorageClass.STANDARD,
                StorageFilePurpose.GENERATED);

        assertThat(file.getProjectId()).isNotNull();
        assertThat(file.getHash()).isEqualTo("ABC123HASH");
    }

    @Test
    void shouldExposeAdminManagementPermissionsAndCrewCreativeFlags() {
        RolePermissionRegistry rolePermissionRegistry = new RolePermissionRegistry();
        assertThat(rolePermissionRegistry.resolve(Role.ADMIN))
                .contains(
                        Permission.BRAND_MANAGE,
                        Permission.PRODUCT_SERVICE_MANAGE,
                        Permission.PROJECT_CAMPAIGN_MANAGE,
                        Permission.CREATIVE_REQUEST_MANAGE,
                        Permission.GENERATED_VERSION_MANAGE);

        WorkspaceMembershipEntity crewMembership = WorkspaceMembershipEntity.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Role.CREW,
                WorkspaceMembershipStatus.ACTIVE,
                java.util.Set.of(Permission.WORKSPACE_VIEW),
                true,
                true,
                java.time.Instant.now(),
                UUID.randomUUID());

        assertThat(crewMembership.canDownloadCreative()).isTrue();
        assertThat(crewMembership.canEditCreative()).isTrue();
    }

    @Test
    void shouldMaintainCreditWalletReserveFinalizeAndRefundLifecycle() {
        UUID workspaceId = UUID.randomUUID();
        CreditWalletEntity wallet = CreditWalletEntity.initialize(workspaceId);
        wallet.addBalance(new BigDecimal("100.00"));
        wallet.reserve(new BigDecimal("25.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.0000");
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo("25.0000");

        wallet.finalizeReservation(new BigDecimal("20.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("80.0000");
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo("5.0000");

        wallet.refundReservation(new BigDecimal("5.00"));
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo("0.0000");

        CreditTransactionEntity transaction = CreditTransactionEntity.create(
                workspaceId,
                CreditTransactionType.RESERVE,
                new BigDecimal("25.00"),
                "CREATIVE_REQUEST",
                UUID.randomUUID(),
                CreditTransactionStatus.COMPLETED);
        assertThat(transaction.getTransactionType()).isEqualTo(CreditTransactionType.RESERVE);
        assertThat(transaction.getAmount()).isEqualByComparingTo("25.0000");
    }

    @Test
    void shouldRejectRefundThatWouldMakeReservedBalanceNegative() {
        CreditWalletEntity wallet = CreditWalletEntity.initialize(UUID.randomUUID());
        wallet.addBalance(new BigDecimal("10.00"));

        assertThatThrownBy(() -> wallet.refundReservation(new BigDecimal("1.00")))
                .isInstanceOf(Exception.class)
                .hasMessage("Reserved balance cannot be negative");
    }
}
