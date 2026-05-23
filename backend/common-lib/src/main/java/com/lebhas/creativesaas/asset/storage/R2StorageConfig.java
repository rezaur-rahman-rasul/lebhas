package com.lebhas.creativesaas.asset.storage;

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

import java.time.Clock;

@Configuration
@ConditionalOnProperty(prefix = "platform.storage", name = "provider", havingValue = "R2", matchIfMissing = true)
@EnableConfigurationProperties({StorageProperties.class, R2StorageProperties.class})
public class R2StorageConfig {

    @Bean
    S3Client r2S3Client(R2StorageProperties properties) {
        var builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                        .build());
        if (properties.getEndpoint() != null) {
            builder.endpointOverride(properties.getEndpoint());
        }
        if (properties.getAccessKey() != null && !properties.getAccessKey().isBlank()
                && properties.getSecretKey() != null && !properties.getSecretKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    @Bean
    S3Presigner r2S3Presigner(R2StorageProperties properties) {
        var builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()));
        if (properties.getEndpoint() != null) {
            builder.endpointOverride(properties.getEndpoint());
        }
        if (properties.getAccessKey() != null && !properties.getAccessKey().isBlank()
                && properties.getSecretKey() != null && !properties.getSecretKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    @Bean
    StorageService storageService(
            StorageProperties storageProperties,
            R2StorageProperties r2StorageProperties,
            StoragePathBuilder storagePathBuilder,
            S3Client r2S3Client,
            S3Presigner r2S3Presigner,
            Clock clock
    ) {
        return new R2StorageProvider(
                storageProperties,
                r2StorageProperties,
                storagePathBuilder,
                r2S3Client,
                r2S3Presigner,
                clock);
    }
}
