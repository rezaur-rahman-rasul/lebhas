package com.lebhas.creativesaas.usage.application;

import com.lebhas.ai.credit.application.ProviderCreditPoolService;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolView;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.credit.domain.CreditTransactionEntity;
import com.lebhas.creativesaas.credit.domain.CreditTransactionStatus;
import com.lebhas.creativesaas.credit.domain.CreditTransactionType;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditTransactionRepository;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.CreditEstimationService;
import com.lebhas.creativesaas.generation.event.CreditLifecycleEventDto;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageCommand;
import com.lebhas.creativesaas.usage.application.dto.CreditPurchaseCreditCommand;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageSettlementCommand;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreditUsageService {

    private static final String DEFAULT_REFERENCE_TYPE = "CREATIVE_REQUEST";

    private final CreditBalanceService creditBalanceService;
    private final CreditLedgerService creditLedgerService;
    private final CreditUsageMapper creditUsageMapper;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreativeRequestRepository creativeRequestRepository;
    private final WorkspaceUsageSummaryService workspaceUsageSummaryService;
    private final CreditEstimationService creditEstimationService;
    private final ObjectProvider<GenerationEventProducer> generationEventProducerProvider;
    private CurrentUserContext currentUserContext;
    private DomainEventPublisher domainEventPublisher;
    private AuditLogService auditLogService;
    private ProviderCreditPoolService providerCreditPoolService;

    public CreditUsageService(
            CreditBalanceService creditBalanceService,
            CreditLedgerService creditLedgerService,
            CreditUsageMapper creditUsageMapper,
            CreditTransactionRepository creditTransactionRepository,
            CreativeRequestRepository creativeRequestRepository,
            WorkspaceUsageSummaryService workspaceUsageSummaryService,
            CreditEstimationService creditEstimationService,
            ObjectProvider<GenerationEventProducer> generationEventProducerProvider
    ) {
        this.creditBalanceService = creditBalanceService;
        this.creditLedgerService = creditLedgerService;
        this.creditUsageMapper = creditUsageMapper;
        this.creditTransactionRepository = creditTransactionRepository;
        this.creativeRequestRepository = creativeRequestRepository;
        this.workspaceUsageSummaryService = workspaceUsageSummaryService;
        this.creditEstimationService = creditEstimationService;
        this.generationEventProducerProvider = generationEventProducerProvider;
    }

    @Autowired(required = false)
    void setCurrentUserContext(CurrentUserContext currentUserContext) {
        this.currentUserContext = currentUserContext;
    }

    @Autowired(required = false)
    void setDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Autowired(required = false)
    void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Autowired(required = false)
    void setProviderCreditPoolService(ProviderCreditPoolService providerCreditPoolService) {
        this.providerCreditPoolService = providerCreditPoolService;
    }

    @Transactional
    public CreditUsageResult addPurchasedCredits(CreditPurchaseCreditCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        BigDecimal amount = normalizePositive(command.creditsAmount(), "creditsAmount");
        String referenceType = referenceType(command.referenceType());
        UUID referenceId = require(command.referenceId(), "referenceId");

        return creditBalanceService.withCreditLock(workspaceId, () -> {
            CreditBalanceService.BalanceMovement movement = creditBalanceService.purchase(workspaceId, amount);
            CreditTransactionEntity transaction = creditTransactionRepository.save(CreditTransactionEntity.create(
                    workspaceId,
                    CreditTransactionType.PURCHASE,
                    amount,
                    referenceType,
                    referenceId,
                    CreditTransactionStatus.COMPLETED));
            CreditLedger ledger = creditLedgerService.append(
                    workspaceId,
                    null,
                    null,
                    null,
                    CreditLedgerTransactionType.PURCHASE,
                    amount,
                    movement.balanceBefore(),
                    movement.balanceAfter(),
                    referenceType,
                    referenceId,
                    normalizeDescription(command.description(), "Credits purchased"),
                    command.createdBy());
            return creditUsageMapper.toUsageResult(
                    ledger,
                    transaction.getId(),
                    movement.wallet(),
                    amount,
                    referenceType,
                    referenceId);
        });
    }

    @Transactional
    public CreditUsageResult adjustCredits(
            UUID workspaceId,
            BigDecimal creditsAmount,
            String referenceType,
            UUID referenceId,
            String description
    ) {
        CurrentUser currentUser = requireMaster();
        UUID effectiveWorkspaceId = require(workspaceId, "workspaceId");
        BigDecimal amount = normalizeNonZero(creditsAmount, "creditsAmount");
        String effectiveReferenceType = referenceType(referenceType == null ? "MASTER_CREDIT_ADJUSTMENT" : referenceType);
        UUID effectiveReferenceId = referenceId == null ? UUID.randomUUID() : referenceId;

        return creditBalanceService.withCreditLock(effectiveWorkspaceId, () -> {
            CreditBalanceService.BalanceMovement movement = creditBalanceService.adjust(effectiveWorkspaceId, amount);
            CreditTransactionEntity transaction = creditTransactionRepository.save(CreditTransactionEntity.create(
                    effectiveWorkspaceId,
                    CreditTransactionType.ADJUSTMENT,
                    amount,
                    effectiveReferenceType,
                    effectiveReferenceId,
                    CreditTransactionStatus.COMPLETED));
            CreditLedger ledger = creditLedgerService.append(
                    effectiveWorkspaceId,
                    null,
                    null,
                    null,
                    CreditLedgerTransactionType.MANUAL_ADJUSTMENT,
                    amount,
                    movement.balanceBefore(),
                    movement.balanceAfter(),
                    effectiveReferenceType,
                    effectiveReferenceId,
                    normalizeDescription(description, "Master credit adjustment"),
                    currentUser.userId());
            publishCreditAdjusted(effectiveWorkspaceId, ledger.getId(), amount, effectiveReferenceType, effectiveReferenceId);
            publishCreditLedgerCreated(effectiveWorkspaceId, ledger.getId(), amount);
            auditCreditAdjusted(effectiveWorkspaceId, ledger.getId(), amount, effectiveReferenceType, effectiveReferenceId);
            return creditUsageMapper.toUsageResult(
                    ledger,
                    transaction.getId(),
                    movement.wallet(),
                    amount,
                    effectiveReferenceType,
                    effectiveReferenceId);
        });
    }

    @Transactional
    public CreditUsageResult reserveCredits(CreditUsageCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        UUID creativeRequestId = require(command.creativeRequestId(), "creativeRequestId");
        CreativeRequestEntity creativeRequest = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(creativeRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        BigDecimal serverCalculatedCredits = calculateCredits(creativeRequest);
        String referenceType = referenceType(command.referenceType());
        UUID referenceId = referenceId(command.referenceId(), creativeRequestId);

        return creditBalanceService.withCreditLock(workspaceId, () -> {
            CreditBalanceService.BalanceMovement movement = creditBalanceService.reserve(workspaceId, serverCalculatedCredits);
            CreditTransactionEntity transaction = creditTransactionRepository.save(CreditTransactionEntity.create(
                    workspaceId,
                    CreditTransactionType.RESERVE,
                    serverCalculatedCredits,
                    referenceType,
                    referenceId,
                    CreditTransactionStatus.COMPLETED));
            CreditLedger ledger = creditLedgerService.append(
                    workspaceId,
                    creativeRequestId,
                    command.generatedVersionId(),
                    command.generationJobId(),
                    CreditLedgerTransactionType.RESERVE,
                    serverCalculatedCredits,
                    movement.balanceBefore(),
                    movement.balanceAfter(),
                    referenceType,
                    referenceId,
                    "Credit reservation for generation",
                    command.createdBy());
            WorkspaceUsageSummary summary = summary(workspaceId);
            summary.recordReservation(serverCalculatedCredits);
            workspaceUsageSummaryService.recordSummaryMutation(summary, referenceType, referenceId, "CREDITS_RESERVED");
            reserveProviderCreditsIfConfigured(serverCalculatedCredits, referenceType, referenceId, command.createdBy());
            publishReserved(workspaceId, creativeRequestId, command.generatedVersionId(), transaction.getId(), serverCalculatedCredits);
            return creditUsageMapper.toUsageResult(
                    ledger,
                    transaction.getId(),
                    movement.wallet(),
                    serverCalculatedCredits,
                    referenceType,
                    referenceId);
        });
    }

    @Transactional
    public CreditUsageResult finalizeCredits(CreditUsageSettlementCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        String referenceType = referenceType(command.referenceType());
        UUID referenceId = referenceId(command.referenceId(), command.creativeRequestId());
        BigDecimal amount = outstandingAmount(workspaceId, referenceType, referenceId);

        return creditBalanceService.withCreditLock(workspaceId, () -> {
            CreditBalanceService.BalanceMovement movement = creditBalanceService.finalizeReservation(workspaceId, amount);
            CreditTransactionEntity transaction = creditTransactionRepository.save(CreditTransactionEntity.create(
                    workspaceId,
                    CreditTransactionType.FINALIZE,
                    amount,
                    referenceType,
                    referenceId,
                    CreditTransactionStatus.COMPLETED));
            CreditLedger ledger = creditLedgerService.append(
                    workspaceId,
                    command.creativeRequestId(),
                    command.generatedVersionId(),
                    command.generationJobId(),
                    CreditLedgerTransactionType.FINALIZE,
                    amount,
                    movement.balanceBefore(),
                    movement.balanceAfter(),
                    referenceType,
                    referenceId,
                    normalizeDescription(command.reason(), "Credit finalized after generation success"),
                    command.createdBy());
            WorkspaceUsageSummary summary = summary(workspaceId);
            summary.recordFinalization(amount);
            workspaceUsageSummaryService.recordSummaryMutation(summary, referenceType, referenceId, "CREDITS_FINALIZED");
            useProviderCreditsIfReserved(amount, referenceType, referenceId, command.createdBy());
            publishFinalized(workspaceId, command.creativeRequestId(), command.generatedVersionId(), reservationId(command, transaction), amount, command.reason());
            return creditUsageMapper.toUsageResult(
                    ledger,
                    reservationId(command, transaction),
                    movement.wallet(),
                    amount,
                    referenceType,
                    referenceId);
        });
    }

    @Transactional
    public CreditUsageResult refundCredits(CreditUsageSettlementCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        String referenceType = referenceType(command.referenceType());
        UUID referenceId = referenceId(command.referenceId(), command.creativeRequestId());
        BigDecimal amount = outstandingAmount(workspaceId, referenceType, referenceId);

        return creditBalanceService.withCreditLock(workspaceId, () -> {
            CreditBalanceService.BalanceMovement movement = creditBalanceService.refundReservation(workspaceId, amount);
            CreditTransactionEntity transaction = creditTransactionRepository.save(CreditTransactionEntity.create(
                    workspaceId,
                    CreditTransactionType.REFUND,
                    amount,
                    referenceType,
                    referenceId,
                    CreditTransactionStatus.COMPLETED));
            CreditLedger ledger = creditLedgerService.append(
                    workspaceId,
                    command.creativeRequestId(),
                    command.generatedVersionId(),
                    command.generationJobId(),
                    CreditLedgerTransactionType.REFUND,
                    amount,
                    movement.balanceBefore(),
                    movement.balanceAfter(),
                    referenceType,
                    referenceId,
                    normalizeDescription(command.reason(), "Credit refunded after generation failure"),
                    command.createdBy());
            WorkspaceUsageSummary summary = summary(workspaceId);
            summary.recordRefund(amount);
            workspaceUsageSummaryService.recordSummaryMutation(summary, referenceType, referenceId, "CREDITS_REFUNDED");
            releaseProviderCreditsIfReserved(amount, referenceType, referenceId, command.createdBy());
            publishRefunded(workspaceId, command.creativeRequestId(), command.generatedVersionId(), reservationId(command, transaction), amount, command.reason());
            return creditUsageMapper.toUsageResult(
                    ledger,
                    reservationId(command, transaction),
                    movement.wallet(),
                    amount,
                    referenceType,
                    referenceId);
        });
    }

    private BigDecimal calculateCredits(CreativeRequestEntity creativeRequest) {
        BigDecimal baseCredits = creditEstimationService.estimate(creativeRequest.getCreativeType());
        int requestedVersions = Math.max(1, creativeRequest.getRequestedVersions());
        return baseCredits.multiply(BigDecimal.valueOf(requestedVersions)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal outstandingAmount(UUID workspaceId, String referenceType, UUID referenceId) {
        BigDecimal outstanding = creditLedgerService.outstandingReservedCredits(workspaceId, referenceType, referenceId);
        if (outstanding.signum() <= 0) {
            throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "No outstanding reserved credits exist for the supplied reference");
        }
        return outstanding;
    }

    private void reserveProviderCreditsIfConfigured(BigDecimal amount, String referenceType, UUID referenceId, UUID createdBy) {
        if (providerCreditPoolService == null) {
            return;
        }
        providerCreditPoolService.listPools()
                .stream()
                .filter(pool -> pool.availableInternalCredits().compareTo(amount) >= 0)
                .findFirst()
                .map(ProviderCreditPoolView::providerId)
                .ifPresent(providerId -> providerCreditPoolService.reserveProviderCredits(providerId, amount, referenceType, referenceId, createdBy));
    }

    private void useProviderCreditsIfReserved(BigDecimal amount, String referenceType, UUID referenceId, UUID createdBy) {
        providerForReservation(referenceType, referenceId)
                .ifPresent(providerId -> providerCreditPoolService.useReservedProviderCredits(providerId, amount, referenceType, referenceId, createdBy));
    }

    private void releaseProviderCreditsIfReserved(BigDecimal amount, String referenceType, UUID referenceId, UUID createdBy) {
        providerForReservation(referenceType, referenceId)
                .ifPresent(providerId -> providerCreditPoolService.releaseReservedProviderCredits(providerId, amount, referenceType, referenceId, createdBy));
    }

    private Optional<UUID> providerForReservation(String referenceType, UUID referenceId) {
        if (providerCreditPoolService == null) {
            return Optional.empty();
        }
        return providerCreditPoolService.findReservedProviderId(referenceType, referenceId);
    }

    private WorkspaceUsageSummary summary(UUID workspaceId) {
        LocalDate usageMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        return workspaceUsageSummaryService.getOrCreateSummary(workspaceId, usageMonth);
    }

    private void publishReserved(UUID workspaceId, UUID creativeRequestId, UUID generatedVersionId, UUID creditReservationId, BigDecimal amount) {
        GenerationEventProducer producer = generationEventProducerProvider.getIfAvailable();
        if (producer != null) {
            producer.publishCreditsReserved(creditEvent(workspaceId, creativeRequestId, generatedVersionId, creditReservationId, amount, "RESERVED", null));
        }
    }

    private void publishFinalized(UUID workspaceId, UUID creativeRequestId, UUID generatedVersionId, UUID creditReservationId, BigDecimal amount, String reason) {
        GenerationEventProducer producer = generationEventProducerProvider.getIfAvailable();
        if (producer != null) {
            producer.publishCreditsFinalized(creditEvent(workspaceId, creativeRequestId, generatedVersionId, creditReservationId, amount, "FINALIZED", reason));
        }
    }

    private void publishRefunded(UUID workspaceId, UUID creativeRequestId, UUID generatedVersionId, UUID creditReservationId, BigDecimal amount, String reason) {
        GenerationEventProducer producer = generationEventProducerProvider.getIfAvailable();
        if (producer != null) {
            producer.publishCreditsRefunded(creditEvent(workspaceId, creativeRequestId, generatedVersionId, creditReservationId, amount, "REFUNDED", reason));
        }
    }

    private CreditLifecycleEventDto creditEvent(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            UUID creditReservationId,
            BigDecimal amount,
            String status,
            String reason
    ) {
        return new CreditLifecycleEventDto(
                workspaceId,
                creativeRequestId,
                generatedVersionId,
                creditReservationId,
                amount,
                status,
                reason,
                Instant.now());
    }

    private UUID reservationId(CreditUsageSettlementCommand command, CreditTransactionEntity fallback) {
        return command.creditReservationId() == null ? fallback.getId() : command.creditReservationId();
    }

    private String referenceType(String referenceType) {
        if (referenceType == null || referenceType.isBlank()) {
            return DEFAULT_REFERENCE_TYPE;
        }
        return referenceType.trim();
    }

    private UUID referenceId(UUID referenceId, UUID fallback) {
        if (referenceId != null) {
            return referenceId;
        }
        return require(fallback, "referenceId");
    }

    private String normalizeDescription(String value, String fallback) {
        String source = value == null || value.isBlank() ? fallback : value;
        String normalized = source.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }

    private BigDecimal normalizePositive(BigDecimal amount, String field) {
        if (amount == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        BigDecimal normalized = amount.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " must be greater than zero");
        }
        return normalized;
    }

    private BigDecimal normalizeNonZero(BigDecimal amount, String field) {
        if (amount == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        BigDecimal normalized = amount.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " must not be zero");
        }
        return normalized;
    }

    private CurrentUser requireMaster() {
        if (currentUserContext == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }

    private void publishCreditAdjusted(UUID workspaceId, UUID ledgerId, BigDecimal amount, String referenceType, UUID referenceId) {
        publishUsageEvent(KafkaTopicConstants.CREDITS_ADJUSTED, workspaceId, ledgerId, amount, referenceType, referenceId);
    }

    private void publishCreditLedgerCreated(UUID workspaceId, UUID ledgerId, BigDecimal amount) {
        publishUsageEvent(KafkaTopicConstants.CREDIT_LEDGER_CREATED, workspaceId, ledgerId, amount, "CREDIT_LEDGER", ledgerId);
    }

    private void publishUsageEvent(String topic, UUID workspaceId, UUID aggregateId, BigDecimal amount, String referenceType, UUID referenceId) {
        if (domainEventPublisher == null) {
            return;
        }
        domainEventPublisher.publish(topic, new BaseDomainEvent(
                topic,
                workspaceId,
                aggregateId,
                Instant.now(),
                Map.of(
                        "amount", amount,
                        "referenceType", referenceType,
                        "referenceId", referenceId.toString())));
    }

    private void auditCreditAdjusted(UUID workspaceId, UUID ledgerId, BigDecimal amount, String referenceType, UUID referenceId) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.appendCurrentUserAction(
                workspaceId,
                "credits.adjusted.%s".formatted(ledgerId),
                AuditActionType.UPDATE,
                AuditOutcome.SUCCESS,
                "CreditLedger",
                ledgerId,
                "Workspace credits adjusted",
                Map.of(
                        "ledgerId", ledgerId.toString(),
                        "amount", amount.toPlainString(),
                        "referenceType", referenceType,
                        "referenceId", referenceId.toString()),
                null,
                null);
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
