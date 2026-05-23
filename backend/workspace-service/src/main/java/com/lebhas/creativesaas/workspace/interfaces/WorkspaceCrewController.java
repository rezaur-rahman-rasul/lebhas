package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.identity.application.dto.InviteCrewCommand;
import com.lebhas.creativesaas.identity.application.dto.InvitationView;
import com.lebhas.creativesaas.workspace.application.CrewManagementService;
import com.lebhas.creativesaas.workspace.application.dto.CrewMemberView;
import com.lebhas.creativesaas.workspace.application.dto.UpdateCrewMemberCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{id}/crew")
@Tag(name = "Workspace Crew")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceCrewController {

    private final CrewManagementService crewManagementService;

    public WorkspaceCrewController(CrewManagementService crewManagementService) {
        this.crewManagementService = crewManagementService;
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAuthority('CREW_INVITE')")
    @Operation(summary = "Invite a crew member into a workspace")
    public ApiResponse<InvitationView> inviteCrew(@PathVariable UUID id, @Valid @RequestBody InviteCrewRequest request) {
        return ApiResponse.success(crewManagementService.inviteCrew(new InviteCrewCommand(
                id,
                request.email(),
                request.role(),
                request.permissions())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CREW_VIEW')")
    @Operation(summary = "List workspace crew members")
    public ApiResponse<List<CrewMemberView>> listCrew(@PathVariable UUID id) {
        return ApiResponse.success(crewManagementService.listCrewMembers(id));
    }

    @GetMapping("/{crewId}")
    @PreAuthorize("hasAuthority('CREW_VIEW')")
    @Operation(summary = "Get workspace crew member details")
    public ApiResponse<CrewMemberView> getCrew(@PathVariable UUID id, @PathVariable UUID crewId) {
        return ApiResponse.success(crewManagementService.getCrewMember(id, crewId));
    }

    @PutMapping("/{crewId}")
    @PreAuthorize("hasAuthority('CREW_UPDATE')")
    @Operation(summary = "Update workspace crew permissions or status")
    public ApiResponse<CrewMemberView> updateCrew(
            @PathVariable UUID id,
            @PathVariable UUID crewId,
            @Valid @RequestBody UpdateCrewMemberRequest request
    ) {
        return ApiResponse.success(crewManagementService.updateCrewMember(new UpdateCrewMemberCommand(
                id,
                crewId,
                request.permissions(),
                request.status())));
    }

    @DeleteMapping("/{crewId}")
    @PreAuthorize("hasAuthority('CREW_REMOVE')")
    @Operation(summary = "Remove a crew member from a workspace")
    public ApiResponse<Void> removeCrew(@PathVariable UUID id, @PathVariable UUID crewId) {
        crewManagementService.removeCrewMember(id, crewId);
        return ApiResponse.success("Crew member removed", null);
    }
}
