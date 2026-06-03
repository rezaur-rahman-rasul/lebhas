package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.operations.application.OperationsReadinessService;
import com.lebhas.creativesaas.operations.application.SystemFeatureToggleService;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.*;
import com.lebhas.creativesaas.operations.domain.SystemFeatureToggleKey;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class MasterOperationsController {
    private final SystemFeatureToggleService toggleService;
    private final OperationsReadinessService readinessService;

    public MasterOperationsController(SystemFeatureToggleService toggleService, OperationsReadinessService readinessService) {
        this.toggleService = toggleService;
        this.readinessService = readinessService;
    }

    @GetMapping("/api/v1/master/operations/overview")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<OverviewView> overview() { return ApiResponse.success(readinessService.overview()); }

    @GetMapping("/api/v1/master/operations/feature-toggles")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<List<ToggleView>> toggles() { return ApiResponse.success(toggleService.list()); }

    @PutMapping("/api/v1/master/operations/feature-toggles/{toggleKey}")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<ToggleView> updateToggle(@PathVariable SystemFeatureToggleKey toggleKey, @RequestBody ToggleUpdateRequest request) {
        return ApiResponse.success(toggleService.update(toggleKey, request.enabled(), request.reason()));
    }

    @PostMapping("/api/v1/master/operations/maintenance-mode/enable")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<MaintenanceStatusView> enableMaintenance(@RequestBody(required = false) ToggleUpdateRequest request) {
        return ApiResponse.success(toggleService.setMaintenanceMode(true, request == null ? null : request.reason()));
    }

    @PostMapping("/api/v1/master/operations/maintenance-mode/disable")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<MaintenanceStatusView> disableMaintenance(@RequestBody(required = false) ToggleUpdateRequest request) {
        return ApiResponse.success(toggleService.setMaintenanceMode(false, request == null ? null : request.reason()));
    }

    @GetMapping("/api/v1/system/maintenance-status")
    public ApiResponse<MaintenanceStatusView> maintenanceStatus() { return ApiResponse.success(toggleService.maintenanceStatus()); }

    @PostMapping("/api/v1/master/smoke-tests/run")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<SmokeTestRunView> runSmokeTests() { return ApiResponse.success(readinessService.runSmokeTests()); }

    @GetMapping("/api/v1/master/smoke-tests")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<List<SmokeTestRunView>> smokeTests() { return ApiResponse.success(readinessService.smokeRuns()); }

    @GetMapping("/api/v1/master/smoke-tests/{runId}")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<SmokeTestRunView> smokeTest(@PathVariable UUID runId) { return ApiResponse.success(readinessService.smokeRun(runId)); }

    @GetMapping("/api/v1/master/go-live/checklist")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<List<ChecklistItemView>> checklist() { return ApiResponse.success(readinessService.checklist()); }

    @PostMapping("/api/v1/master/go-live/checklist")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<ChecklistItemView> createChecklist(@RequestBody ChecklistCommand command) { return ApiResponse.success(readinessService.createChecklistItem(command)); }

    @PutMapping("/api/v1/master/go-live/checklist/{itemId}")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<ChecklistItemView> updateChecklist(@PathVariable UUID itemId, @RequestBody ChecklistCommand command) { return ApiResponse.success(readinessService.updateChecklistItem(itemId, command)); }

    @PostMapping("/api/v1/master/go-live/checklist/{itemId}/complete")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<ChecklistItemView> completeChecklist(@PathVariable UUID itemId) { return ApiResponse.success(readinessService.completeChecklistItem(itemId)); }

    @PostMapping("/api/v1/master/go-live/checklist/{itemId}/block")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<ChecklistItemView> blockChecklist(@PathVariable UUID itemId, @RequestBody ChecklistCommand command) { return ApiResponse.success(readinessService.blockChecklistItem(itemId, command.reason())); }

    @GetMapping("/api/v1/master/go-live/readiness")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<ReadinessView> readiness() { return ApiResponse.success(readinessService.goLiveReadiness()); }

    @GetMapping("/api/v1/master/go-live-readiness")
    @PreAuthorize("hasAnyRole('MASTER') or hasAuthority('MASTER_ADMIN')")
    public ApiResponse<DetailedReadinessView> detailedReadiness() { return ApiResponse.success(readinessService.detailedGoLiveReadiness()); }

    @GetMapping("/api/v1/master/security/readiness")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<SecurityReadinessView> security() { return ApiResponse.success(readinessService.securityReadiness()); }

    @PostMapping("/api/v1/master/operations/data-integrity/run")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<DataIntegrityRunView> runIntegrity() { return ApiResponse.success(readinessService.runDataIntegrity()); }

    @GetMapping("/api/v1/master/operations/data-integrity/runs")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<List<DataIntegrityRunView>> integrityRuns() { return ApiResponse.success(readinessService.integrityRuns()); }

    @GetMapping("/api/v1/master/operations/data-integrity/runs/{runId}")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<DataIntegrityRunView> integrityRun(@PathVariable UUID runId) { return ApiResponse.success(readinessService.integrityRun(runId)); }

    @GetMapping("/health/dependencies")
    public ApiResponse<DependencyHealthView> dependencies() { return ApiResponse.success(readinessService.dependencyHealth()); }
}
