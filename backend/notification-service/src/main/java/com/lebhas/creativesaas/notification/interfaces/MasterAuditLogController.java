package com.lebhas.creativesaas.notification.interfaces;

import com.lebhas.creativesaas.auditlog.application.AuditLogView;
import com.lebhas.creativesaas.auditlog.application.AuditQueryService;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/master/audit-logs")
@Tag(name = "Master Audit Logs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MASTER')")
public class MasterAuditLogController {

    private final AuditQueryService auditQueryService;

    public MasterAuditLogController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @Operation(summary = "List system audit logs")
    public ApiResponse<PagedResult<AuditLogView>> auditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audit log from date must be before to date.");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 200));
        List<AuditLogView> logs = auditQueryService.listMasterAuditLogs((safePage + 1) * safeSize);
        List<AuditLogView> filtered = logs.stream()
                .filter(log -> module == null || module.isBlank() || String.valueOf(log.entityType()).equalsIgnoreCase(module))
                .filter(log -> action == null || action.isBlank() || String.valueOf(log.actionType()).equalsIgnoreCase(action))
                .filter(log -> actor == null || actor.isBlank() || String.valueOf(log.actorUserId()).equalsIgnoreCase(actor))
                .filter(log -> severity == null || severity.isBlank() || String.valueOf(log.outcome()).equalsIgnoreCase(severity))
                .filter(log -> from == null || !log.auditAt().isBefore(from))
                .filter(log -> to == null || !log.auditAt().isAfter(to))
                .toList();
        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        return ApiResponse.success(new PagedResult<>(
                filtered.subList(fromIndex, toIndex),
                filtered.size(),
                (int) Math.ceil((double) filtered.size() / safeSize),
                safePage,
                safeSize,
                safePage == 0,
                toIndex >= filtered.size()));
    }
}
