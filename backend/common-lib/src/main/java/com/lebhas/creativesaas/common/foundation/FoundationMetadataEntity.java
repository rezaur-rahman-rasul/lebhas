package com.lebhas.creativesaas.common.foundation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "foundation_metadata", schema = "platform")
public class FoundationMetadataEntity {

    @Id
    @Column(name = "metadata_key", nullable = false, length = 80)
    private String metadataKey;

    @Column(name = "metadata_value", nullable = false, length = 200)
    private String metadataValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FoundationMetadataEntity() {
    }

    public String getMetadataKey() {
        return metadataKey;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
