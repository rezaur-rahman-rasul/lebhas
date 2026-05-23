package com.lebhas.creativesaas.generatedversion.domain;

public enum GenerationStatus {
    QUEUED,
    PROCESSING,
    READY,
    FAILED,
    @Deprecated
    COMPLETED,
    @Deprecated
    CANCELLED;

    public boolean isReady() {
        return this == READY || this == COMPLETED;
    }
}
