package com.lebhas.creativesaas.storage.application;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StoragePathBuilderTest {

    private final StoragePathBuilder storagePathBuilder = new StoragePathBuilder();

    @Test
    void shouldBuildStructuredPathsForEachStoragePurpose() {
        UUID workspaceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID projectId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID fileId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        assertThat(storagePathBuilder.buildRawPath(workspaceId, projectId, fileId))
                .isEqualTo("raw/workspaces/11111111-1111-1111-1111-111111111111/projects/22222222-2222-2222-2222-222222222222/33333333-3333-3333-3333-333333333333");
        assertThat(storagePathBuilder.buildProcessedPath(workspaceId, projectId, fileId))
                .isEqualTo("processed/workspaces/11111111-1111-1111-1111-111111111111/projects/22222222-2222-2222-2222-222222222222/33333333-3333-3333-3333-333333333333");
        assertThat(storagePathBuilder.buildGeneratedPath(workspaceId, projectId, fileId))
                .isEqualTo("generated/workspaces/11111111-1111-1111-1111-111111111111/projects/22222222-2222-2222-2222-222222222222/33333333-3333-3333-3333-333333333333");
        assertThat(storagePathBuilder.buildThumbnailPath(workspaceId, projectId, fileId))
                .isEqualTo("thumbnails/workspaces/11111111-1111-1111-1111-111111111111/projects/22222222-2222-2222-2222-222222222222/33333333-3333-3333-3333-333333333333");
    }
}
