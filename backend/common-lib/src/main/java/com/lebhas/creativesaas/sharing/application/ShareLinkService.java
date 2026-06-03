package com.lebhas.creativesaas.sharing.application;

import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidationService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.asset.application.AssetEventPublisher;
import com.lebhas.creativesaas.sharing.application.dto.CreateRevisedShareLinkCommand;
import com.lebhas.creativesaas.sharing.application.dto.RevisedShareLinkView;
import com.lebhas.creativesaas.sharing.cache.ShareLinkCacheEntry;
import com.lebhas.creativesaas.sharing.cache.ShareLinkCacheService;
import com.lebhas.creativesaas.sharing.domain.ShareLink;
import com.lebhas.creativesaas.sharing.event.ShareLinkCreatedEvent;
import com.lebhas.creativesaas.sharing.event.ShareLinkEventProducer;
import com.lebhas.creativesaas.sharing.infrastructure.persistence.ShareLinkRepository;
import com.lebhas.creativesaas.sharing.validation.ShareLinkValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShareLinkService {

    private static final int TOKEN_ATTEMPTS = 10;

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final SecureTokenService secureTokenService;
    private final ShareLinkRepository shareLinkRepository;
    private final ShareLinkValidationService shareLinkValidationService;
    private final ApprovalPermissionValidationService approvalPermissionValidationService;
    private final ShareLinkCacheService shareLinkCacheService;
    private final ShareLinkMapper shareLinkMapper;
    private final ShareLinkEventProducer shareLinkEventProducer;
    private final AssetEventPublisher eventPublisher;
    private final Clock clock;

    public ShareLinkService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            GeneratedVersionRepository generatedVersionRepository,
            SecureTokenService secureTokenService,
            ShareLinkRepository shareLinkRepository,
            ShareLinkValidationService shareLinkValidationService,
            ApprovalPermissionValidationService approvalPermissionValidationService,
            ShareLinkCacheService shareLinkCacheService,
            ShareLinkMapper shareLinkMapper,
            ShareLinkEventProducer shareLinkEventProducer,
            AssetEventPublisher eventPublisher,
            Clock clock
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.generatedVersionRepository = generatedVersionRepository;
        this.secureTokenService = secureTokenService;
        this.shareLinkRepository = shareLinkRepository;
        this.shareLinkValidationService = shareLinkValidationService;
        this.approvalPermissionValidationService = approvalPermissionValidationService;
        this.shareLinkCacheService = shareLinkCacheService;
        this.shareLinkMapper = shareLinkMapper;
        this.shareLinkEventProducer = shareLinkEventProducer;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public RevisedShareLinkView createRevisedShareLink(CreateRevisedShareLinkCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(command.workspaceId());
        validateExpiry(command.expiresAt());
        String rawToken = resolveUniqueRevisedToken(command.token());
        String tokenHash = secureTokenService.hashToken(rawToken);
        shareLinkValidationService.requireShareLinkCreationAllowed(access, command.generatedVersionId(), tokenHash);

        ShareLink shareLink = ShareLink.create(
                command.workspaceId(),
                command.generatedVersionId(),
                tokenHash,
                command.expiresAt(),
                access.currentUser().userId());
        ShareLink saved = shareLinkRepository.save(shareLink);
        shareLinkCacheService.cacheShareLink(saved);
        publishShareLinkCreated(saved);
        publishGeneratedVersionShareEvent(KafkaTopicConstants.GENERATED_VERSION_SHARE_LINK_CREATED, saved, access.currentUser().userId());
        return shareLinkMapper.toCreationView(saved, rawToken);
    }

    @Transactional(readOnly = true)
    public RevisedShareLinkView getRevisedShareLinkByToken(UUID workspaceId, String token) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                approvalPermissionValidationService.requireApprovalVisibility(workspaceId);
        String normalizedToken = normalizeToken(token);
        String tokenHash = secureTokenService.hashToken(normalizedToken);
        ShareLinkCacheEntry cached = shareLinkCacheService.getShareLink(tokenHash)
                .filter(entry -> access.workspace().getId().equals(entry.workspaceId()))
                .orElse(null);
        if (cached != null) {
            return shareLinkMapper.toView(cached);
        }
        ShareLink shareLink = shareLinkValidationService.requireShareLinkTokenBelongsToWorkspace(
                access.workspace().getId(),
                tokenHash);
        validateShareLink(shareLink);
        shareLinkCacheService.cacheShareLink(shareLink);
        return shareLinkMapper.toView(shareLink);
    }

    @Transactional(readOnly = true)
    public ResolvedShareLink resolvePublicShareLink(String token, String password) {
        if (StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Password-protected share links are not supported");
        }
        String normalizedToken = normalizeToken(token);
        String tokenHash = secureTokenService.hashToken(normalizedToken);
        ShareLinkCacheEntry cached = shareLinkCacheService.getShareLink(tokenHash).orElse(null);
        if (cached != null) {
            validateShareLink(cached);
            GeneratedVersionEntity generatedVersion = requireShareableGeneratedVersion(
                    cached.workspaceId(),
                    cached.generatedVersionId());
            return new ResolvedShareLink(
                    cached.workspaceId(),
                    cached.generatedVersionId(),
                    cached.tokenHash(),
                    cached.expiresAt(),
                    false,
                    cached.accessCount(),
                    cached.createdBy(),
                    generatedVersion);
        }
        ShareLink shareLink = shareLinkValidationService.requireShareLinkToken(tokenHash);
        validateShareLink(shareLink);
        GeneratedVersionEntity generatedVersion = requireShareableGeneratedVersion(
                shareLink.getWorkspaceId(),
                shareLink.getGeneratedVersionId());
        shareLinkCacheService.cacheShareLink(shareLink);
        return new ResolvedShareLink(
                shareLink.getWorkspaceId(),
                shareLink.getGeneratedVersionId(),
                shareLink.getTokenHash(),
                shareLink.getExpiresAt(),
                false,
                shareLink.getAccessCount(),
                shareLink.getCreatedBy(),
                generatedVersion);
    }

    @Transactional(readOnly = true)
    public List<RevisedShareLinkView> listForGeneratedVersion(UUID workspaceId, UUID generatedVersionId) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                approvalPermissionValidationService.requireApprovalVisibility(workspaceId);
        requireShareableGeneratedVersion(access.workspace().getId(), generatedVersionId);
        return shareLinkRepository
                .findAllByWorkspaceIdAndGeneratedVersionIdOrderByCreatedAtDesc(access.workspace().getId(), generatedVersionId)
                .stream()
                .map(shareLinkMapper::toView)
                .toList();
    }

    @Transactional
    public RevisedShareLinkView revoke(UUID workspaceId, UUID generatedVersionId, UUID shareLinkId) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
        ShareLink shareLink = shareLinkValidationService.requireShareLinkBelongsToWorkspace(access.workspace().getId(), shareLinkId);
        if (!generatedVersionId.equals(shareLink.getGeneratedVersionId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Share link not found");
        }
        shareLink.revoke(access.currentUser().userId());
        ShareLink saved = shareLinkRepository.save(shareLink);
        shareLinkCacheService.invalidateShareLink(saved);
        publishGeneratedVersionShareEvent(KafkaTopicConstants.GENERATED_VERSION_SHARE_LINK_REVOKED, saved, access.currentUser().userId());
        return shareLinkMapper.toView(saved);
    }

    @Transactional(readOnly = true)
    public GeneratedVersionEntity requireGeneratedVersionForWorkspace(UUID workspaceId, UUID generatedVersionId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
        return requireShareableGeneratedVersion(workspaceId, generatedVersionId);
    }

    private GeneratedVersionEntity requireShareableGeneratedVersion(UUID workspaceId, UUID generatedVersionId) {
        GeneratedVersionEntity generatedVersion = generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
        if (generatedVersion.getStatus() != GeneratedVersionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Generated version is not active");
        }
        if (generatedVersion.getGenerationStatus() == null || !generatedVersion.getGenerationStatus().isReady()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Generated version is not ready for download or sharing");
        }
        if (generatedVersion.getStorageFileId() == null) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND);
        }
        return generatedVersion;
    }

    private void validateShareLink(ShareLink shareLink) {
        if (shareLink.getExpiresAt() != null && shareLink.getExpiresAt().isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Share link has expired");
        }
        if (shareLink.isRevoked()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Share link has been revoked");
        }
    }

    private void validateShareLink(ShareLinkCacheEntry shareLink) {
        if (shareLink.expiresAt() != null && shareLink.expiresAt().isBefore(clock.instant())) {
            shareLinkCacheService.invalidateShareLink(shareLink);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Share link has expired");
        }
        if (shareLink.revoked()) {
            shareLinkCacheService.invalidateShareLink(shareLink);
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Share link has been revoked");
        }
    }

    private void validateExpiry(Instant expiresAt) {
        if (expiresAt == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Share link expiry must be provided");
        }
        if (!expiresAt.isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Share link expiry must be in the future");
        }
    }

    private String resolveUniqueRevisedToken(String requestedToken) {
        if (StringUtils.hasText(requestedToken)) {
            String normalized = requestedToken.trim();
            shareLinkValidationService.requireTokenHashAvailable(secureTokenService.hashToken(normalized));
            return normalized;
        }
        for (int attempt = 0; attempt < TOKEN_ATTEMPTS; attempt++) {
            String token = secureTokenService.generatePublicToken();
            if (!shareLinkRepository.existsByTokenHash(secureTokenService.hashToken(token))) {
                return token;
            }
        }
        throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "A unique share link token could not be generated");
    }

    private void publishShareLinkCreated(ShareLink shareLink) {
        ShareLinkCreatedEvent event = new ShareLinkCreatedEvent(
                shareLink.getId(),
                shareLink.getWorkspaceId(),
                shareLink.getGeneratedVersionId(),
                shareLink.getTokenHash(),
                shareLink.getCreatedBy(),
                shareLink.getExpiresAt(),
                Instant.now(clock));
        shareLinkEventProducer.publishCreated(event);
    }

    private void publishGeneratedVersionShareEvent(String topic, ShareLink shareLink, UUID actorUserId) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publish(topic, shareLink.getWorkspaceId(), shareLink.getGeneratedVersionId(), Map.of(
                "workspaceId", shareLink.getWorkspaceId().toString(),
                "generatedVersionId", shareLink.getGeneratedVersionId().toString(),
                "shareLinkId", shareLink.getId().toString(),
                "actorUserId", actorUserId == null ? "" : actorUserId.toString()));
    }

    private String normalizeToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return token.trim();
    }

    public record ResolvedShareLink(
            UUID workspaceId,
            UUID generatedVersionId,
            String token,
            Instant expiresAt,
            boolean passwordProtected,
            long accessCount,
            UUID createdByUserId,
            GeneratedVersionEntity generatedVersion
    ) {
    }
}
