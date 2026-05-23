package com.lebhas.creativesaas.storage.config;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.storage.foundation")
public class StorageConfigProperties {

    private StorageProvider defaultProvider = StorageProvider.R2;
    private String defaultBucket = "creative-saas-assets";
    private String defaultCdnBaseUrl;
    private final R2Properties r2 = new R2Properties();

    public StorageProvider getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(StorageProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    public String getDefaultCdnBaseUrl() {
        return defaultCdnBaseUrl;
    }

    public void setDefaultCdnBaseUrl(String defaultCdnBaseUrl) {
        this.defaultCdnBaseUrl = defaultCdnBaseUrl;
    }

    public R2Properties getR2() {
        return r2;
    }

    public static class R2Properties {
        private String region = "auto";
        private String endpoint;
        private String accountId;
        private String bucket;
        private String publicBaseUrl;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }
}
