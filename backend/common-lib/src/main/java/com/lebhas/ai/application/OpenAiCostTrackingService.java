package com.lebhas.ai.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.dto.OpenAiCostSyncResult;
import com.lebhas.ai.credit.application.CreditValuePolicyService;
import com.lebhas.ai.credit.application.ProviderCreditPoolService;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolCommand;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Transactional
public class OpenAiCostTrackingService {

    private static final String OPENAI_PROVIDER_CODE = "OPENAI";
    private static final String OPENAI_COSTS_ENDPOINT = "https://api.openai.com/v1/organization/costs";

    private final AiToolProviderRepository providerRepository;
    private final AiCredentialEncryptionService encryptionService;
    private final ObjectProvider<CreditValuePolicyService> creditValuePolicyService;
    private final ObjectProvider<ProviderCreditPoolService> providerCreditPoolService;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCostTrackingService(
            AiToolProviderRepository providerRepository,
            AiCredentialEncryptionService encryptionService,
            ObjectProvider<CreditValuePolicyService> creditValuePolicyService,
            ObjectProvider<ProviderCreditPoolService> providerCreditPoolService,
            AuditLogService auditLogService,
            Clock clock
    ) {
        this.providerRepository = providerRepository;
        this.encryptionService = encryptionService;
        this.creditValuePolicyService = creditValuePolicyService;
        this.providerCreditPoolService = providerCreditPoolService;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public OpenAiCostSyncResult syncCosts(UUID providerId) {
        AiToolProvider provider = requireOpenAiProvider(providerId);
        BigDecimal previousSpend = zero(provider.getTotalCostSpentUsd());
        BigDecimal previousBalance = currentEstimatedBalance(provider);
        Instant syncedAt = clock.instant();
        String adminKey = encryptionService.decryptNullable(provider.getOpenAiAdminApiKeyEncrypted());
        if (adminKey == null || adminKey.isBlank()) {
            return failed(provider, "Invalid Admin API Key", null, previousSpend, previousBalance, syncedAt);
        }
        if (!adminKey.trim().startsWith("sk-admin-")) {
            return failed(provider, "Invalid Admin API Key", 401, previousSpend, previousBalance, syncedAt);
        }

        try {
            BigDecimal reportedSpend = calculateSpend(provider, adminKey.trim());
            BigDecimal spend = zero(provider.getTotalCostSpentUsd()).max(reportedSpend);
            BigDecimal balance = calculateEstimatedBalance(provider, spend);
            provider.applyOpenAiCostSync(spend, balance, syncedAt);
            providerRepository.save(provider);
            BigDecimal credits = convertToInternalCredits(provider, balance);
            syncProviderCreditPool(provider, balance, credits);
            OpenAiCostSyncResult result = new OpenAiCostSyncResult(
                    provider.getId(),
                    provider.getProviderCode(),
                    true,
                    "OpenAI costs synced successfully",
                    200,
                    previousSpend,
                    spend,
                    previousBalance,
                    balance,
                    credits,
                    syncedAt);
            audit(provider, result, AuditOutcome.SUCCESS);
            return result;
        } catch (OpenAiCostsApiException exception) {
            return failed(provider, messageForStatus(exception.statusCode()), exception.statusCode(), previousSpend, previousBalance, syncedAt);
        } catch (IOException exception) {
            return failed(provider, "Provider Error", null, previousSpend, previousBalance, syncedAt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failed(provider, "Provider Error", null, previousSpend, previousBalance, syncedAt);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateSpend(UUID providerId) {
        AiToolProvider provider = requireOpenAiProvider(providerId);
        String adminKey = encryptionService.decryptNullable(provider.getOpenAiAdminApiKeyEncrypted());
        if (adminKey == null || adminKey.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid Admin API Key");
        }
        try {
            return calculateSpend(provider, adminKey.trim());
        } catch (OpenAiCostsApiException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, messageForStatus(exception.statusCode()));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Provider Error");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Provider Error");
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateEstimatedBalance(UUID providerId) {
        AiToolProvider provider = requireOpenAiProvider(providerId);
        return calculateEstimatedBalance(provider, zero(provider.getTotalCostSpentUsd()));
    }

    @Transactional(readOnly = true)
    public BigDecimal convertToInternalCredits(UUID providerId) {
        AiToolProvider provider = requireOpenAiProvider(providerId);
        return convertToInternalCredits(provider, currentEstimatedBalance(provider));
    }

    public void syncEnabledProviders() {
        providerRepository.findAllByDeletedFalseOrderByProviderNameAsc().stream()
                .filter(provider -> OPENAI_PROVIDER_CODE.equals(provider.getProviderCode()))
                .filter(AiToolProvider::isCostSyncEnabled)
                .forEach(provider -> syncCosts(provider.getId()));
    }

    public void recordOpenAiCreativeGenerationCost() {
        AiToolProvider provider = providerRepository.findByProviderCodeAndDeletedFalse(OPENAI_PROVIDER_CODE)
                .orElse(null);
        if (provider == null) {
            return;
        }
        BigDecimal cost = openAiCreativeProviderCost();
        provider.recordOpenAiSpend(cost, clock.instant());
        providerRepository.save(provider);
        BigDecimal balance = currentEstimatedBalance(provider);
        BigDecimal credits = convertToInternalCredits(provider, balance);
        syncProviderCreditPool(provider, balance, credits);
    }

    private BigDecimal calculateSpend(AiToolProvider provider, String adminKey) throws IOException, InterruptedException {
        BigDecimal total = BigDecimal.ZERO;
        String nextPage = null;
        int guard = 0;
        do {
            HttpResponse<String> response = httpClient.send(costsRequest(provider, adminKey, nextPage), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OpenAiCostsApiException(response.statusCode());
            }
            Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            total = total.add(sumCosts(body));
            Object next = body.get("next_page");
            nextPage = next == null || String.valueOf(next).isBlank() ? null : String.valueOf(next);
            guard++;
        } while (nextPage != null && guard < 50);
        return total.setScale(4, RoundingMode.HALF_UP);
    }

    private HttpRequest costsRequest(AiToolProvider provider, String adminKey, String nextPage) {
        String endpoint = OPENAI_COSTS_ENDPOINT
                + "?start_time=" + startTime(provider)
                + "&limit=180";
        if (nextPage != null) {
            endpoint += "&page=" + URLEncoder.encode(nextPage, StandardCharsets.UTF_8);
        }
        return HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + adminKey)
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private long startTime(AiToolProvider provider) {
        LocalDate topUpDate = provider.getProviderTopUpDate();
        if (topUpDate == null) {
            return Instant.now(clock).minus(Duration.ofDays(365)).getEpochSecond();
        }
        return topUpDate.atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    private BigDecimal sumCosts(Map<String, Object> body) {
        Object data = body.get("data");
        if (!(data instanceof List<?> buckets)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Object bucket : buckets) {
            if (!(bucket instanceof Map<?, ?> bucketMap)) {
                continue;
            }
            Object results = bucketMap.get("results");
            if (!(results instanceof List<?> resultList)) {
                continue;
            }
            for (Object result : resultList) {
                if (result instanceof Map<?, ?> resultMap) {
                    total = total.add(amountValue(resultMap.get("amount")));
                }
            }
        }
        return total;
    }

    private BigDecimal amountValue(Object amount) {
        if (!(amount instanceof Map<?, ?> amountMap)) {
            return BigDecimal.ZERO;
        }
        Object value = amountMap.get("value");
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateEstimatedBalance(AiToolProvider provider, BigDecimal spend) {
        BigDecimal topUp = zero(provider.getProviderTopUpAmountUsd());
        BigDecimal remaining = topUp.subtract(zero(spend));
        return normalize(remaining.signum() < 0 ? BigDecimal.ZERO : remaining);
    }

    private BigDecimal openAiCreativeProviderCost() {
        try {
            CreditValuePolicyService policyService = creditValuePolicyService.getIfAvailable();
            if (policyService == null) {
                return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
            return policyService.requireActivePolicy()
                    .providerCost(null)
                    .setScale(4, RoundingMode.HALF_UP);
        } catch (RuntimeException exception) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal convertToInternalCredits(AiToolProvider provider, BigDecimal balance) {
        try {
            CreditValuePolicyService policyService = creditValuePolicyService.getIfAvailable();
            if (policyService == null) {
                return null;
            }
            BigDecimal creditValueUsd = policyService.requireActivePolicy().getCreditUsdValue();
            if (creditValueUsd == null || creditValueUsd.signum() <= 0) {
                return null;
            }
            return zero(balance).divide(creditValueUsd, 4, RoundingMode.FLOOR);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void syncProviderCreditPool(AiToolProvider provider, BigDecimal balance, BigDecimal internalCredits) {
        if (internalCredits == null) {
            return;
        }
        try {
            ProviderCreditPoolService poolService = providerCreditPoolService.getIfAvailable();
            if (poolService == null) {
                return;
            }
            poolService.createOrReplacePool(provider.getId(), new ProviderCreditPoolCommand(
                    "USD",
                    normalize(balance),
                    normalize(internalCredits),
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)));
        } catch (RuntimeException ignored) {
            // Keep the real OpenAI cost sync values even if existing pool reservations block a pool reset.
        }
    }

    private OpenAiCostSyncResult failed(
            AiToolProvider provider,
            String message,
            Integer httpStatus,
            BigDecimal previousSpend,
            BigDecimal previousBalance,
            Instant syncedAt
    ) {
        OpenAiCostSyncResult result = new OpenAiCostSyncResult(
                provider.getId(),
                provider.getProviderCode(),
                false,
                message,
                httpStatus,
                previousSpend,
                previousSpend,
                previousBalance,
                previousBalance,
                convertToInternalCredits(provider, previousBalance),
                syncedAt);
        audit(provider, result, AuditOutcome.FAILURE);
        return result;
    }

    private AiToolProvider requireOpenAiProvider(UUID providerId) {
        AiToolProvider provider = providerRepository.findByIdAndDeletedFalse(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
        if (!OPENAI_PROVIDER_CODE.equals(provider.getProviderCode())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Cost sync is available only for OpenAI providers");
        }
        return provider;
    }

    private BigDecimal currentEstimatedBalance(AiToolProvider provider) {
        if (provider.getEstimatedRemainingBalanceUsd() != null) {
            return normalize(provider.getEstimatedRemainingBalanceUsd());
        }
        return calculateEstimatedBalance(provider, zero(provider.getTotalCostSpentUsd()));
    }

    private String messageForStatus(int statusCode) {
        return switch (statusCode) {
            case 401 -> "Invalid Admin API Key";
            case 403 -> "Admin Key Missing Permission";
            case 429 -> "Rate Limited";
            default -> statusCode >= 500 ? "Provider Error" : "OpenAI costs sync failed with HTTP " + statusCode;
        };
    }

    private void audit(AiToolProvider provider, OpenAiCostSyncResult result, AuditOutcome outcome) {
        if (auditLogService == null) {
            return;
        }
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("providerCode", provider.getProviderCode());
            metadata.put("syncTime", result.syncedAt());
            metadata.put("previousSpendUsd", result.previousSpendUsd());
            metadata.put("newSpendUsd", result.totalCostSpentUsd());
            metadata.put("previousBalanceUsd", result.previousBalanceUsd());
            metadata.put("newBalanceUsd", result.estimatedRemainingBalanceUsd());
            metadata.put("syncResult", result.message());
            metadata.put("httpStatus", result.httpStatus());
            auditLogService.appendCurrentPlatformAction(
                    "provider.openai-cost-sync." + provider.getId() + "." + clock.millis(),
                    AuditActionType.PROCESS,
                    outcome,
                    "PROVIDER",
                    provider.getId(),
                    "OpenAI cost sync " + (result.success() ? "completed" : "failed"),
                    metadata,
                    null,
                    null);
        } catch (RuntimeException ignored) {
            // Scheduled sync may run without an authenticated current user.
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : normalize(value);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private static final class OpenAiCostsApiException extends RuntimeException {
        private final int statusCode;

        private OpenAiCostsApiException(int statusCode) {
            this.statusCode = statusCode;
        }

        private int statusCode() {
            return statusCode;
        }
    }
}
