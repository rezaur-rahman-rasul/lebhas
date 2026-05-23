package com.lebhas.creativesaas.asset.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Day4R2StorageProviderTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3Presigner s3Presigner = mock(S3Presigner.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneOffset.UTC);

    private R2StorageProvider provider;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setBucket("creative-r2-assets");
        storageProperties.setSignedUrlTtl(Duration.ofMinutes(15));

        R2StorageProperties r2StorageProperties = new R2StorageProperties();
        r2StorageProperties.setBucket("creative-r2-assets");
        r2StorageProperties.setPublicBaseUrl(URI.create("https://cdn.example.com"));

        provider = new R2StorageProvider(
                storageProperties,
                r2StorageProperties,
                new StoragePathBuilder(),
                s3Client,
                s3Presigner,
                clock);
    }

    @Test
    void uploadToR2Works() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        StorageObjectResponse response = provider.upload(new StorageObjectRequest(
                workspaceId,
                projectId,
                assetId,
                null,
                "launch.png",
                "image/png",
                4L,
                null,
                () -> new ByteArrayInputStream(new byte[]{1, 2, 3, 4})));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));

        assertThat(requestCaptor.getValue().bucket()).isEqualTo("creative-r2-assets");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        assertThat(requestCaptor.getValue().key())
                .isEqualTo("assets/workspaces/%s/projects/%s/%s/launch.png".formatted(workspaceId, projectId, assetId));
        assertThat(response.provider()).isEqualTo(com.lebhas.creativesaas.asset.domain.StorageProvider.R2);
        assertThat(response.bucket()).isEqualTo("creative-r2-assets");
        assertThat(response.objectKey())
                .isEqualTo("assets/workspaces/%s/projects/%s/%s/launch.png".formatted(workspaceId, projectId, assetId));
        assertThat(response.cdnUrl())
                .isEqualTo("https://cdn.example.com/assets/workspaces/%s/projects/%s/%s/launch.png"
                        .formatted(workspaceId, projectId, assetId));
    }

    @Test
    void signedUrlGenerationWorks() {
        PresignedGetObjectRequest presignedRequest = PresignedGetObjectRequest.builder()
                .expiration(clock.instant().plus(Duration.ofMinutes(15)))
                .isBrowserExecutable(true)
                .signedHeaders(Map.of("host", List.of("signed.example.com")))
                .httpRequest(SdkHttpRequest.builder()
                        .method(SdkHttpMethod.GET)
                        .uri(URI.create("https://signed.example.com/creative-r2-assets/assets/workspaces/file.png"))
                        .build())
                .build();
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        SignedUrlResponse response = provider.generateSignedUrl(new SignedUrlRequest(
                null,
                "assets/workspaces/file.png",
                Duration.ofMinutes(15),
                "file.png",
                true,
                "image/png"));

        ArgumentCaptor<GetObjectPresignRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(requestCaptor.capture());

        assertThat(requestCaptor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(requestCaptor.getValue().getObjectRequest().bucket()).isEqualTo("creative-r2-assets");
        assertThat(requestCaptor.getValue().getObjectRequest().key()).isEqualTo("assets/workspaces/file.png");
        assertThat(requestCaptor.getValue().getObjectRequest().responseContentDisposition())
                .isEqualTo("attachment; filename=\"file.png\"");
        assertThat(response.url()).isEqualTo("https://signed.example.com/creative-r2-assets/assets/workspaces/file.png");
        assertThat(response.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
        assertThat(response.cdnUrl()).isEqualTo("https://cdn.example.com/assets/workspaces/file.png");
    }

    @Test
    void assetDeletionWorks() {
        provider.delete("creative-r2-assets", "assets/workspaces/file.png");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        assertThat(requestCaptor.getValue().bucket()).isEqualTo("creative-r2-assets");
        assertThat(requestCaptor.getValue().key()).isEqualTo("assets/workspaces/file.png");
    }
}
