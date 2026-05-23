package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.storage.config.StorageConfigProperties;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.storage.domain.StorageFilePurpose;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageFileServiceTest {

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;

    @Mock
    private StorageFileRepository storageFileRepository;

    @Mock
    private StorageFileViewMapper storageFileViewMapper;

    private StorageFileService storageFileService;

    @BeforeEach
    void setUp() {
        StorageConfigProperties storageConfigProperties = new StorageConfigProperties();
        storageConfigProperties.setDefaultBucket("foundation-bucket");
        storageConfigProperties.setDefaultCdnBaseUrl("https://cdn.example.com/media");
        storageFileService = new StorageFileService(
                workspaceAuthorizationService,
                storageFileRepository,
                storageFileViewMapper,
                storageConfigProperties);
    }

    @Test
    void shouldNormalizeHashWhenFindingDuplicates() {
        UUID workspaceId = UUID.randomUUID();
        StorageFileEntity duplicate = StorageFileEntity.create(
                workspaceId,
                UUID.randomUUID(),
                StorageProvider.S3,
                "foundation-bucket",
                "raw/workspaces/w/projects/p/file",
                null,
                "image/png",
                "png",
                512L,
                "abc123",
                null,
                null,
                null,
                com.lebhas.creativesaas.storage.domain.StorageClass.STANDARD,
                StorageFilePurpose.RAW);
        when(storageFileRepository.findFirstByWorkspaceIdAndHashAndDeletedFalse(workspaceId, "abc123"))
                .thenReturn(Optional.of(duplicate));

        Optional<StorageFileEntity> result = storageFileService.findDuplicateByHash(workspaceId, "  ABC123  ");

        assertThat(result).containsSame(duplicate);
        verify(storageFileRepository).findFirstByWorkspaceIdAndHashAndDeletedFalse(workspaceId, "abc123");
    }

    @Test
    void shouldRegisterGeneratedOutputWithHashBucketAndCdn() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        byte[] content = "generated-image".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(storageFileRepository.save(any(StorageFileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, StorageFileEntity.class));

        StorageFileEntity stored = storageFileService.registerGeneratedOutput(
                workspaceId,
                projectId,
                StorageProvider.S3,
                null,
                "generated/workspaces/%s/projects/%s/%s".formatted(workspaceId, projectId, UUID.randomUUID()),
                null,
                "image/png",
                "png",
                content.length,
                1080,
                1080,
                null,
                content);

        ArgumentCaptor<StorageFileEntity> captor = ArgumentCaptor.forClass(StorageFileEntity.class);
        verify(storageFileRepository).save(captor.capture());

        assertThat(stored.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(stored.getProjectId()).isEqualTo(projectId);
        assertThat(stored.getBucket()).isEqualTo("foundation-bucket");
        assertThat(stored.getCdnUrl()).isEqualTo("https://cdn.example.com/media/" + stored.getObjectKey());
        assertThat(stored.getHash()).isEqualTo(sha256(content));
        assertThat(stored.getFilePurpose()).isEqualTo(StorageFilePurpose.GENERATED);
    }

    private String sha256(byte[] content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(content));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
