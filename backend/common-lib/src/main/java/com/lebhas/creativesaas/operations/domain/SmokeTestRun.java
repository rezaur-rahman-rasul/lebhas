package com.lebhas.creativesaas.operations.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "smoke_test_runs", schema = "platform")
public class SmokeTestRun extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private SmokeTestRunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "results", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> results = new LinkedHashMap<>();

    protected SmokeTestRun() {
    }

    public static SmokeTestRun running() {
        SmokeTestRun run = new SmokeTestRun();
        run.status = SmokeTestRunStatus.RUNNING;
        run.startedAt = Instant.now();
        return run;
    }

    public void complete(SmokeTestRunStatus status, Map<String, Object> results) {
        this.status = status;
        this.results = new LinkedHashMap<>(results == null ? Map.of() : results);
        this.completedAt = Instant.now();
    }

    public SmokeTestRunStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Map<String, Object> getResults() { return Map.copyOf(results); }
}
