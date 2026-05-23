package com.lebhas.creativesaas.common.health;

import com.lebhas.creativesaas.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Tag(name = "Foundation Health")
public class PlatformHealthController {

    private final ApplicationAvailability availability;
    private final String serviceName;

    public PlatformHealthController(
            ApplicationAvailability availability,
            @Value("${spring.application.name:application}") String serviceName
    ) {
        this.availability = availability;
        this.serviceName = serviceName;
    }

    @GetMapping("/health")
    @Operation(summary = "Returns service health")
    ResponseEntity<ApiResponse<HealthStatusResponse>> health() {
        return ResponseEntity.ok(ApiResponse.success("Service is healthy", status("UP", "RUNNING")));
    }

    @GetMapping("/liveness")
    @Operation(summary = "Returns Kubernetes liveness state")
    ResponseEntity<ApiResponse<HealthStatusResponse>> liveness() {
        LivenessState state = availability.getState(LivenessState.class, LivenessState.CORRECT);
        HttpStatus status = state == LivenessState.CORRECT ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .body(ApiResponse.success("Liveness state resolved", status(status, state.name())));
    }

    @GetMapping("/readiness")
    @Operation(summary = "Returns Kubernetes readiness state")
    ResponseEntity<ApiResponse<HealthStatusResponse>> readiness() {
        ReadinessState state = availability.getState(ReadinessState.class, ReadinessState.ACCEPTING_TRAFFIC);
        HttpStatus status = state == ReadinessState.ACCEPTING_TRAFFIC ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .body(ApiResponse.success("Readiness state resolved", status(status, state.name())));
    }

    private HealthStatusResponse status(HttpStatus httpStatus, String state) {
        return status(httpStatus.is2xxSuccessful() ? "UP" : "OUT_OF_SERVICE", state);
    }

    private HealthStatusResponse status(String status, String state) {
        return new HealthStatusResponse(serviceName, status, state, Instant.now());
    }
}
