package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({StorageProperties.class, R2StorageProperties.class})
public class StorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "platform.storage", name = "provider", havingValue = "LOCAL")
    LocalStorageService localStorageService(
            StorageProperties storageProperties,
            StoragePathResolver storagePathResolver,
            LocalSignedAssetUrlService localSignedAssetUrlService
    ) {
        return new LocalStorageService(storageProperties, storagePathResolver, localSignedAssetUrlService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.storage", name = "provider", havingValue = "S3")
    S3Client s3Client(StorageProperties storageProperties) {
        return buildS3Client(storageProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.storage", name = "provider", havingValue = "S3")
    S3Presigner s3Presigner(StorageProperties storageProperties) {
        return buildS3Presigner(storageProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.storage", name = "provider", havingValue = "S3")
    StorageService s3StorageService(
            StorageProperties storageProperties,
            StoragePathResolver storagePathResolver,
            S3Client s3Client,
            S3Presigner s3Presigner
    ) {
        return new S3StorageService(StorageProvider.S3, storageProperties, storagePathResolver, s3Client, s3Presigner);
    }

    private S3Client buildS3Client(StorageProperties storageProperties) {
        var builder = S3Client.builder()
                .region(Region.of(storageProperties.getS3().getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storageProperties.getS3().isPathStyleAccessEnabled())
                        .build());
        if (storageProperties.getS3().getEndpoint() != null) {
            builder.endpointOverride(storageProperties.getS3().getEndpoint());
        }
        if (storageProperties.getS3().getAccessKey() != null && !storageProperties.getS3().getAccessKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            storageProperties.getS3().getAccessKey(),
                            storageProperties.getS3().getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    private S3Presigner buildS3Presigner(StorageProperties storageProperties) {
        var builder = S3Presigner.builder()
                .region(Region.of(storageProperties.getS3().getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storageProperties.getS3().isPathStyleAccessEnabled())
                        .build());
        if (storageProperties.getS3().getEndpoint() != null) {
            builder.endpointOverride(storageProperties.getS3().getEndpoint());
        }
        if (storageProperties.getS3().getAccessKey() != null && !storageProperties.getS3().getAccessKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            storageProperties.getS3().getAccessKey(),
                            storageProperties.getS3().getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
