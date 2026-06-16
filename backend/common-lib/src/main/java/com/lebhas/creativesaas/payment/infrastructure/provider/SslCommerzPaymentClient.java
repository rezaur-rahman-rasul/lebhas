package com.lebhas.creativesaas.payment.infrastructure.provider;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.application.PaymentCredentialEncryptionService;
import com.lebhas.creativesaas.payment.application.PaymentSessionRequest;
import com.lebhas.creativesaas.payment.application.PaymentSessionResponse;
import com.lebhas.creativesaas.payment.application.PaymentVerificationRequest;
import com.lebhas.creativesaas.payment.application.PaymentVerificationResponse;
import com.lebhas.creativesaas.payment.application.PaymentWebhookVerificationResult;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class SslCommerzPaymentClient extends AbstractFoundationPaymentClient {

    private static final String DEFAULT_SANDBOX_BASE_URL = "https://sandbox.sslcommerz.com";
    private static final String DEFAULT_LIVE_BASE_URL = "https://securepay.sslcommerz.com";

    private final PaymentCredentialEncryptionService encryptionService;
    private final RestClient restClient;

    public SslCommerzPaymentClient(PaymentCredentialEncryptionService encryptionService, RestClient.Builder restClientBuilder) {
        this.encryptionService = encryptionService;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.SSLCOMMERZ;
    }

    @Override
    public PaymentSessionResponse createSession(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            PaymentSessionRequest request
    ) {
        String storeId = required(configuration.getMerchantId(), "SSLCommerz store id is not configured");
        String storePassword = required(
                encryptionService.decryptNullable(configuration.getEncryptedSecret()),
                "SSLCommerz store password is not configured");
        String transactionId = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : request.idempotencyKey().trim();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("store_id", storeId);
        form.add("store_passwd", storePassword);
        form.add("total_amount", request.amount().toPlainString());
        form.add("currency", request.currency());
        form.add("tran_id", transactionId);
        form.add("success_url", required(configuration.getSuccessUrl(), "SSLCommerz success URL is not configured"));
        form.add("fail_url", required(configuration.getFailureUrl(), "SSLCommerz fail URL is not configured"));
        form.add("cancel_url", required(configuration.getCancelUrl(), "SSLCommerz cancel URL is not configured"));
        form.add("ipn_url", ipnUrl(configuration));
        form.add("cus_name", "Lebhas Admin");
        form.add("cus_email", "billing@lebhas.local");
        form.add("cus_add1", "Lebhas Workspace");
        form.add("cus_city", "Dhaka");
        form.add("cus_country", "Bangladesh");
        form.add("cus_phone", "01700000000");
        form.add("shipping_method", "NO");
        form.add("product_name", request.paymentPurpose().name());
        form.add("product_category", "Digital Credits");
        form.add("product_profile", "non-physical-goods");
        form.add("value_a", request.referenceType());
        form.add("value_b", request.referenceId().toString());
        form.add("value_c", request.workspaceId().toString());
        form.add("value_d", request.userId().toString());

        Map<String, Object> response = restClient.post()
                .uri(baseUrl(configuration) + "/gwprocess/v4/api.php")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        Map<String, String> payload = stringify(response);
        String status = payload.getOrDefault("status", "");
        String redirectUrl = firstPresent(payload, "GatewayPageURL", "redirectGatewayURL", "directPaymentURLBank");
        if (!"SUCCESS".equalsIgnoreCase(status) || redirectUrl == null || redirectUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "SSLCommerz checkout session could not be created");
        }

        return new PaymentSessionResponse(
                provider.getId(),
                provider.getCode(),
                provider.getProviderType(),
                configuration.getEnvironmentType(),
                transactionId,
                transactionId,
                redirectUrl,
                PaymentTransactionStatus.PENDING,
                "SSLCommerz checkout session created",
                withoutSecrets(payload)
        );
    }

    @Override
    public PaymentVerificationResponse verifyPayment(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            PaymentVerificationRequest request
    ) {
        Map<String, String> payload = request.providerPayload() == null ? Map.of() : request.providerPayload();
        String validationId = firstPresent(payload, "val_id", "validationId");
        String providerTransactionId = firstPresent(payload, "tran_id", "bank_tran_id", "providerTransactionId");
        if (validationId == null || validationId.isBlank()) {
            return new PaymentVerificationResponse(
                    provider.getId(),
                    provider.getCode(),
                    provider.getProviderType(),
                    configuration.getEnvironmentType(),
                    request.providerSessionId(),
                    providerTransactionId,
                    PaymentTransactionStatus.PENDING,
                    null,
                    null,
                    "SSLCommerz validation id missing",
                    withoutSecrets(payload)
            );
        }

        Map<String, String> validation = validateWithGateway(configuration, validationId);
        PaymentTransactionStatus status = sslStatus(validation.get("status"));
        String verifiedTransactionId = firstPresent(validation, "tran_id", "bank_tran_id", "providerTransactionId");
        return new PaymentVerificationResponse(
                provider.getId(),
                provider.getCode(),
                provider.getProviderType(),
                configuration.getEnvironmentType(),
                request.providerSessionId(),
                verifiedTransactionId == null ? providerTransactionId : verifiedTransactionId,
                status,
                parseDecimal(validation.get("amount")),
                validation.get("currency"),
                status == PaymentTransactionStatus.SUCCESS ? "SSLCommerz payment verified" : "SSLCommerz payment not verified as paid",
                withoutSecrets(validation)
        );
    }

    @Override
    public PaymentWebhookVerificationResult verifyWebhook(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            String payload,
            String signature
    ) {
        Map<String, String> callbackPayload = parsePayload(payload);
        String validationId = firstPresent(callbackPayload, "val_id", "validationId");
        String providerTransactionId = firstPresent(callbackPayload, "tran_id", "bank_tran_id", "providerTransactionId");
        PaymentTransactionStatus callbackStatus = callbackStatus(callbackPayload);
        if (validationId == null || validationId.isBlank()) {
            if (callbackStatus == PaymentTransactionStatus.FAILED || callbackStatus == PaymentTransactionStatus.CANCELLED) {
                return new PaymentWebhookVerificationResult(
                        true,
                        PaymentWebhookVerificationStatus.VERIFIED,
                        providerTransactionId,
                        "SSLCOMMERZ_CALLBACK",
                        callbackStatus,
                        null,
                        withoutSecrets(callbackPayload)
                );
            }
            return new PaymentWebhookVerificationResult(
                    false,
                    PaymentWebhookVerificationStatus.FAILED,
                    providerTransactionId,
                    "SSLCOMMERZ_CALLBACK",
                    callbackStatus,
                    "SSLCommerz validation id missing",
                    withoutSecrets(callbackPayload)
            );
        }

        Map<String, String> validation = validateWithGateway(configuration, validationId);
        PaymentTransactionStatus status = sslStatus(validation.get("status"));
        String verifiedTransactionId = firstPresent(validation, "tran_id", "bank_tran_id", "providerTransactionId");
        boolean verified = status == PaymentTransactionStatus.SUCCESS;
        return new PaymentWebhookVerificationResult(
                verified,
                verified ? PaymentWebhookVerificationStatus.VERIFIED : PaymentWebhookVerificationStatus.FAILED,
                verifiedTransactionId == null ? providerTransactionId : verifiedTransactionId,
                "SSLCOMMERZ_CALLBACK",
                status,
                verified ? null : "SSLCommerz validation did not confirm paid status",
                withoutSecrets(merge(callbackPayload, validation))
        );
    }

    private Map<String, String> validateWithGateway(PaymentProviderConfiguration configuration, String validationId) {
        String storeId = required(configuration.getMerchantId(), "SSLCommerz store id is not configured");
        String storePassword = required(
                encryptionService.decryptNullable(configuration.getEncryptedSecret()),
                "SSLCommerz store password is not configured");
        String url = UriComponentsBuilder
                .fromUriString(baseUrl(configuration) + "/validator/api/validationserverAPI.php")
                .queryParam("val_id", validationId)
                .queryParam("store_id", storeId)
                .queryParam("store_passwd", storePassword)
                .queryParam("format", "json")
                .build()
                .toUriString();
        Map<String, Object> response = restClient.get().uri(url).retrieve().body(Map.class);
        return stringify(response);
    }

    private PaymentTransactionStatus callbackStatus(Map<String, String> payload) {
        String status = firstPresent(payload, "status", "paymentStatus");
        if (status == null) {
            return PaymentTransactionStatus.PENDING;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "VALID", "VALIDATED", "SUCCESS" -> PaymentTransactionStatus.PENDING;
            case "FAILED", "FAILURE" -> PaymentTransactionStatus.FAILED;
            case "CANCELLED", "CANCELED", "CANCEL" -> PaymentTransactionStatus.CANCELLED;
            default -> PaymentTransactionStatus.PENDING;
        };
    }

    private PaymentTransactionStatus sslStatus(String status) {
        if (status == null) {
            return PaymentTransactionStatus.PENDING;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "VALID", "VALIDATED" -> PaymentTransactionStatus.SUCCESS;
            case "FAILED", "FAILURE" -> PaymentTransactionStatus.FAILED;
            case "CANCELLED", "CANCELED", "CANCEL" -> PaymentTransactionStatus.CANCELLED;
            default -> PaymentTransactionStatus.PENDING;
        };
    }

    private String baseUrl(PaymentProviderConfiguration configuration) {
        if (configuration.getApiBaseUrl() != null && !configuration.getApiBaseUrl().isBlank()) {
            return configuration.getApiBaseUrl().replaceAll("/+$", "");
        }
        return switch (configuration.getEnvironmentType()) {
            case LIVE -> DEFAULT_LIVE_BASE_URL;
            case SANDBOX -> DEFAULT_SANDBOX_BASE_URL;
        };
    }

    private String ipnUrl(PaymentProviderConfiguration configuration) {
        String successUrl = required(configuration.getSuccessUrl(), "SSLCommerz success URL is not configured");
        int index = successUrl.indexOf("/payments/sslcommerz/");
        if (index > 0) {
            return successUrl.substring(0, index) + "/payments/sslcommerz/ipn";
        }
        return successUrl;
    }

    private Map<String, String> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        String trimmed = payload.trim();
        Map<String, String> parsed = new LinkedHashMap<>();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            String body = trimmed.substring(1, trimmed.length() - 1);
            for (String part : body.split(",")) {
                int split = part.indexOf(':');
                if (split > 0) {
                    parsed.put(stripJson(part.substring(0, split)), stripJson(part.substring(split + 1)));
                }
            }
            return parsed;
        }
        for (String part : trimmed.split("&")) {
            int split = part.indexOf('=');
            if (split > 0) {
                parsed.put(urlDecode(part.substring(0, split)), urlDecode(part.substring(split + 1)));
            }
        }
        return parsed;
    }

    private String stripJson(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.replace("\\\"", "\"");
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private Map<String, String> stringify(Map<String, Object> values) {
        if (values == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                result.put(key, String.valueOf(value));
            }
        });
        return result;
    }

    private Map<String, String> merge(Map<String, String> left, Map<String, String> right) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (left != null) {
            merged.putAll(left);
        }
        if (right != null) {
            merged.putAll(right);
        }
        return merged;
    }

    private Map<String, String> withoutSecrets(Map<String, String> payload) {
        Map<String, String> safe = new LinkedHashMap<>();
        if (payload == null) {
            return Map.of();
        }
        payload.forEach((key, value) -> {
            String normalized = key.toLowerCase(Locale.ROOT);
            if (!normalized.contains("pass") && !normalized.contains("secret") && !normalized.contains("key")) {
                safe.put(key, value);
            }
        });
        return Map.copyOf(safe);
    }

    private String firstPresent(Map<String, String> payload, String... keys) {
        for (String key : keys) {
            String value = payload.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, message);
        }
        return value.trim();
    }
}
