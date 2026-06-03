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
@Table(name = "data_integrity_runs", schema = "platform")
public class DataIntegrityRun extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private DataIntegrityRunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "issue_count", nullable = false)
    private long issueCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "results", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> results = new LinkedHashMap<>();

    protected DataIntegrityRun() {
    }

    public static DataIntegrityRun running() {
        DataIntegrityRun run = new DataIntegrityRun();
        run.status = DataIntegrityRunStatus.RUNNING;
        run.startedAt = Instant.now();
        return run;
    }

    public void complete(Map<String, Object> results, long issueCount) {
        this.results = new LinkedHashMap<>(results == null ? Map.of() : results);
        this.issueCount = issueCount;
        this.status = issueCount == 0 ? DataIntegrityRunStatus.PASSED : DataIntegrityRunStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public DataIntegrityRunStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public long getIssueCount() { return issueCount; }
    public Map<String, Object> getResults() { return Map.copyOf(results); }
}
