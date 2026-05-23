package com.lebhas.creativesaas.identity.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.common.security.jwt.JwtProperties;
import com.lebhas.creativesaas.common.security.session.OpaqueTokenService;
import com.lebhas.creativesaas.identity.application.dto.InviteCrewCommand;
import com.lebhas.creativesaas.identity.application.dto.InvitationView;
import com.lebhas.creativesaas.identity.domain.InvitationEntity;
import com.lebhas.creativesaas.identity.domain.InvitationStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.InvitationRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.workspace.application.WorkspacePermissionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CurrentUserContext currentUserContext;
    private final OpaqueTokenService opaqueTokenService;
    private final JwtProperties jwtProperties;
    private final WorkspacePermissionPolicy workspacePermissionPolicy;
    private final Clock clock;

    public InvitationService(
            InvitationRepository invitationRepository,
            UserRepository userRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CurrentUserContext currentUserContext,
            OpaqueTokenService opaqueTokenService,
            JwtProperties jwtProperties,
            WorkspacePermissionPolicy workspacePermissionPolicy,
            Clock clock
    ) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.currentUserContext = currentUserContext;
        this.opaqueTokenService = opaqueTokenService;
        this.jwtProperties = jwtProperties;
        this.workspacePermissionPolicy = workspacePermissionPolicy;
        this.clock = clock;
    }

    @Transactional
    public InvitationView inviteCrew(InviteCrewCommand command) {
        if (command.role() != Role.CREW) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only crew invitations are supported in this foundation");
        }
        Set<Permission> permissions = workspacePermissionPolicy.normalizeCrewPermissions(command.permissions());
        String email = normalizeEmail(command.email());
        if (userRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        var currentUser = currentUserContext.requireCurrentUser();
        var workspaceId = workspaceAuthorizationService.requirePermission(command.workspaceId(), Permission.CREW_INVITE).workspace().getId();
        OpaqueTokenService.IssuedOpaqueToken token = opaqueTokenService.issue();
        Instant expiresAt = clock.instant().plus(jwtProperties.getInvitationTokenTtl());
        InvitationEntity invitation = invitationRepository.save(InvitationEntity.create(
                token.tokenId(),
                workspaceId,
                email,
                command.role(),
                currentUser.userId(),
                token.hashedSecret(),
                expiresAt,
                permissions));
        return new InvitationView(
                token.value(),
                workspaceId,
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getPermissions(),
                expiresAt,
                InvitationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public PendingInvitation resolvePendingInvitation(String rawToken, String expectedEmail) {
        OpaqueTokenService.ParsedOpaqueToken parsedToken = opaqueTokenService.parse(rawToken, ErrorCode.INVITATION_INVALID);
        InvitationEntity invitation = invitationRepository.findByTokenIdAndDeletedFalse(parsedToken.tokenId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_INVALID));
        if (!opaqueTokenService.matches(invitation.getTokenHash(), parsedToken.hashedSecret())) {
            throw new BusinessException(ErrorCode.INVITATION_INVALID);
        }
        if (invitation.getStatus(clock.instant()) != InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITATION_INVALID);
        }
        if (!invitation.getEmail().equals(normalizeEmail(expectedEmail))) {
            throw new BusinessException(ErrorCode.INVITATION_INVALID, "Invitation email does not match the registration email");
        }
        return new PendingInvitation(invitation);
    }

    @Transactional
    public void markAccepted(InvitationEntity invitation) {
        invitation.markAccepted(clock.instant());
        invitationRepository.save(invitation);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record PendingInvitation(InvitationEntity invitation) {
    }
}
