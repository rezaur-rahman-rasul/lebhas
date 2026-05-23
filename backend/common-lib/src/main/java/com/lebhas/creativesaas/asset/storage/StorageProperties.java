package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "platform.storage")
public class StorageProperties {

    private StorageProvider provider = StorageProvider.R2;
    private String bucket = "creative-saas-assets";
    private Duration signedUrlTtl = Duration.ofMinutes(15);
    private final S3Properties s3 = new S3Properties();
    private final LocalProperties local = new LocalProperties();

    public StorageProvider getProvider() {
        return provider;
    }

    public void setProvider(StorageProvider provider) {
        this.provider = provider;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public Duration getSignedUrlTtl() {
        return signedUrlTtl;
    }

    public void setSignedUrlTtl(Duration signedUrlTtl) {
        this.signedUrlTtl = signedUrlTtl;
    }

    public S3Properties getS3() {
        return s3;
    }

    public LocalProperties getLocal() {
        return local;
    }

    public static class S3Properties {
        private String region = "ap-southeast-1";
        private URI endpoint;
        private String accessKey;
        private String secretKey;
        private boolean pathStyleAccessEnabled = true;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public URI getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(URI endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isPathStyleAccessEnabled() {
            return pathStyleAccessEnabled;
        }

        public void setPathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
            this.pathStyleAccessEnabled = pathStyleAccessEnabled;
        }
    }

    public static class LocalProperties {
        private Path rootPath = Path.of("./var/storage");
        private URI baseUrl = URI.create("http://localhost:8084");
        private String signingSecret = "creative-saas-local-asset-url-secret";

        public Path getRootPath() {
            return rootPath;
        }

        public void setRootPath(Path rootPath) {
            this.rootPath = rootPath;
        }

        public URI getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getSigningSecret() {
            return signingSecret;
        }

        public void setSigningSecret(String signingSecret) {
            this.signingSecret = signingSecret;
        }
    }
}
