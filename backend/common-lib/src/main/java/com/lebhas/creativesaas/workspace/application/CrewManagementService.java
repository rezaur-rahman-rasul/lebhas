package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.InvitationService;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.identity.application.dto.InviteCrewCommand;
import com.lebhas.creativesaas.identity.application.dto.InvitationView;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.workspace.application.dto.CrewMemberView;
import com.lebhas.creativesaas.workspace.application.dto.UpdateCrewMemberCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CrewManagementService {

    private final InvitationService invitationService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final UserRepository userRepository;
    private final WorkspacePermissionPolicy workspacePermissionPolicy;
    private final WorkspaceViewMapper workspaceViewMapper;
    private final WorkspaceActivityLogger workspaceActivityLogger;

    public CrewManagementService(
            InvitationService invitationService,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            UserRepository userRepository,
            WorkspacePermissionPolicy workspacePermissionPolicy,
            WorkspaceViewMapper workspaceViewMapper,
            WorkspaceActivityLogger workspaceActivityLogger
    ) {
        this.invitationService = invitationService;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.userRepository = userRepository;
        this.workspacePermissionPolicy = workspacePermissionPolicy;
        this.workspaceViewMapper = workspaceViewMapper;
        this.workspaceActivityLogger = workspaceActivityLogger;
    }

    @Transactional
    public InvitationView inviteCrew(InviteCrewCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requirePermission(command.workspaceId(), Permission.CREW_INVITE);
        InvitationView invitation = invitationService.inviteCrew(command);
        workspaceActivityLogger.logCrewInvited(
                access.workspace().getId(),
                access.currentUser().userId(),
                invitation.email(),
                invitation.permissions());
        return invitation;
    }

    @Transactional(readOnly = true)
    public List<CrewMemberView> listCrewMembers(UUID workspaceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREW_VIEW);
        List<WorkspaceMembershipEntity> memberships = workspaceMembershipRepository.findAllByWorkspaceIdAndDeletedFalse(workspaceId).stream()
                .filter(membership -> membership.getRole() == com.lebhas.creativesaas.common.security.Role.CREW)
                .toList();
        Map<UUID, UserEntity> usersById = userRepository.findAllById(
                        memberships.stream().map(WorkspaceMembershipEntity::getUserId).toList()).stream()
                .filter(user -> !user.isDeleted())
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return memberships.stream()
                .map(membership -> toCrewMemberView(usersById.get(membership.getUserId()), membership))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(CrewMemberView::joinedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public CrewMemberView getCrewMember(UUID workspaceId, UUID crewUserId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREW_VIEW);
        WorkspaceMembershipEntity membership = loadCrewMembership(workspaceId, crewUserId);
        UserEntity user = userRepository.findByIdAndDeletedFalse(crewUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_MEMBER_NOT_FOUND));
        return toCrewMemberView(user, membership);
    }

    @Transactional
    public CrewMemberView updateCrewMember(UpdateCrewMemberCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requirePermission(command.workspaceId(), Permission.CREW_UPDATE);
        WorkspaceMembershipEntity membership = loadCrewMembership(command.workspaceId(), command.crewUserId());
        if (command.status() != WorkspaceMembershipStatus.ACTIVE && command.status() != WorkspaceMembershipStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Crew status can only be set to ACTIVE or SUSPENDED");
        }
        membership.replacePermissions(workspacePermissionPolicy.normalizeCrewPermissions(command.permissions()));
        membership.changeStatus(command.status());
        workspaceMembershipRepository.save(membership);
        UserEntity user = userRepository.findByIdAndDeletedFalse(command.crewUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_MEMBER_NOT_FOUND));
        Set<Permission> permissions = workspacePermissionPolicy.resolveEffectivePermissions(membership.getRole(), membership.getPermissions());
        workspaceActivityLogger.logCrewUpdated(
                access.workspace().getId(),
                access.currentUser().userId(),
                command.crewUserId(),
                permissions,
                membership.getStatus().name());
        return workspaceViewMapper.toCrewMemberView(user, membership, permissions);
    }

    @Transactional
    public void removeCrewMember(UUID workspaceId, UUID crewUserId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREW_REMOVE);
        WorkspaceMembershipEntity membership = loadCrewMembership(workspaceId, crewUserId);
        membership.changeStatus(WorkspaceMembershipStatus.REVOKED);
        membership.markDeleted();
        workspaceMembershipRepository.save(membership);
        workspaceActivityLogger.logCrewRemoved(access.workspace().getId(), access.currentUser().userId(), crewUserId);
    }

    private WorkspaceMembershipEntity loadCrewMembership(UUID workspaceId, UUID crewUserId) {
        WorkspaceMembershipEntity membership = workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndDeletedFalse(workspaceId, crewUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_MEMBER_NOT_FOUND));
        if (membership.getRole() != com.lebhas.creativesaas.common.security.Role.CREW) {
            throw new BusinessException(ErrorCode.WORKSPACE_MEMBER_NOT_FOUND);
        }
        return membership;
    }

    private CrewMemberView toCrewMemberView(UserEntity user, WorkspaceMembershipEntity membership) {
        if (user == null) {
            return null;
        }
        Set<Permission> permissions = workspacePermissionPolicy.resolveEffectivePermissions(membership.getRole(), membership.getPermissions());
        return workspaceViewMapper.toCrewMemberView(user, membership, permissions);
    }
}
