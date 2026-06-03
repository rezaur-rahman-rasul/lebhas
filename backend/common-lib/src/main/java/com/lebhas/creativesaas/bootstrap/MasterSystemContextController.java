package com.lebhas.creativesaas.bootstrap;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/master/system")
@Tag(name = "Master System")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MASTER')")
public class MasterSystemContextController {

    private final CurrentUserContext currentUserContext;
    private final String applicationName;

    public MasterSystemContextController(
            CurrentUserContext currentUserContext,
            @Value("${spring.application.name:application}") String applicationName
    ) {
        this.currentUserContext = currentUserContext;
        this.applicationName = applicationName;
    }

    @GetMapping("/context")
    @Operation(summary = "Get basic master system context")
    public ApiResponse<MasterSystemContextView> context() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return ApiResponse.success(new MasterSystemContextView(
                new MasterUserContextView(
                        currentUser.userId(),
                        currentUser.email(),
                        "MASTER"),
                new BackendReadinessView(
                        applicationName,
                        "UP",
                        "DAY_1_FOUNDATION",
                        Instant.now(),
                        Map.of(
                                "auth", true,
                                "profile", true,
                                "workspace", true,
                                "audit", true,
                                "redisFoundation", true,
                                "kafkaFoundation", true))));
    }

    public record MasterSystemContextView(
            MasterUserContextView currentUser,
            BackendReadinessView readiness
    ) {
    }

    public record MasterUserContextView(
            java.util.UUID userId,
            String email,
            String role
    ) {
    }

    public record BackendReadinessView(
            String service,
            String status,
            String foundation,
            Instant checkedAt,
            Map<String, Boolean> capabilities
    ) {
    }
}
