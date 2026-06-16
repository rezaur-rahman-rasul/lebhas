package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.payment.application.PaymentWebhookService;
import com.lebhas.creativesaas.payment.application.dto.PaymentWebhookCommand;
import com.lebhas.creativesaas.payment.application.dto.PaymentWebhookProcessingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment Gateway Callbacks")
public class PaymentGatewayCallbackController {

    private static final String PAYMENT_SIGNATURE_HEADER = "X-Payment-Signature";

    private final PaymentWebhookService paymentWebhookService;

    public PaymentGatewayCallbackController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @GetMapping("/sslcommerz/success")
    @Operation(summary = "Receive SSLCommerz success callback")
    public ApiResponse<PaymentWebhookProcessingResult> sslcommerzSuccess(HttpServletRequest request) {
        return receive("SSLCOMMERZ", withStatus(request, "VALID"), null);
    }

    @PostMapping(value = "/sslcommerz/success", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive SSLCommerz success callback")
    public ApiResponse<PaymentWebhookProcessingResult> sslcommerzSuccess(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(value = PAYMENT_SIGNATURE_HEADER, required = false) String signature
    ) {
        return receive("SSLCOMMERZ", payload(request, body, "VALID"), signature);
    }

    @GetMapping("/sslcommerz/fail")
    @Operation(summary = "Receive SSLCommerz fail callback")
    public ApiResponse<PaymentWebhookProcessingResult> sslcommerzFail(HttpServletRequest request) {
        return receive("SSLCOMMERZ", withStatus(request, "FAILED"), null);
    }

    @PostMapping(value = "/sslcommerz/fail", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive SSLCommerz fail callback")
    public ApiResponse<PaymentWebhookProcessingResult> sslcommerzFail(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(value = PAYMENT_SIGNATURE_HEADER, required = false) String signature
    ) {
        return receive("SSLCOMMERZ", payload(request, body, "FAILED"), signature);
    }

    @GetMapping("/sslcommerz/cancel")
    @Operation(summary = "Receive SSLCommerz cancel callback")
    public ApiResponse<PaymentWebhookProcessingResult> sslcommerzCancel(HttpServletRequest request) {
        return receive("SSLCOMMERZ", withStatus(request, "CANCELLED"), null);
    }

    @PostMapping(value = "/sslcommerz/cancel", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive SSLCommerz cancel callback")
    public ApiResponse<PaymentWebhookProcessingResult> sslcommerzCancel(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(value = PAYMENT_SIGNATURE_HEADER, required = false) String signature
    ) {
        return receive("SSLCOMMERZ", payload(request, body, "CANCELLED"), signature);
    }

    @GetMapping("/sslcommerz/ipn")
    @Operation(summary = "Receive SSLCommerz IPN")
    public ApiResponse<PaymentWebhookProcessingResult> sslcommerzIpn(HttpServletRequest request) {
        return receive("SSLCOMMERZ", queryPayload(request), null);
    }

    @PostMapping(value = "/sslcommerz/ipn", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive SSLCommerz IPN")
    public ApiResponse<PaymentWebhookProcessingResult> sslcommerzIpn(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(value = PAYMENT_SIGNATURE_HEADER, required = false) String signature
    ) {
        return receive("SSLCOMMERZ", payload(request, body, null), signature);
    }

    @GetMapping("/bkash/callback")
    @Operation(summary = "Receive bKash callback")
    public ApiResponse<PaymentWebhookProcessingResult> bkashCallback(HttpServletRequest request) {
        return receive("BKASH", queryPayload(request), null);
    }

    @PostMapping(value = "/bkash/callback", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive bKash callback")
    public ApiResponse<PaymentWebhookProcessingResult> bkashCallback(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(value = PAYMENT_SIGNATURE_HEADER, required = false) String signature
    ) {
        return receive("BKASH", payload(request, body, null), signature);
    }

    @GetMapping("/nagad/callback")
    @Operation(summary = "Receive Nagad callback")
    public ApiResponse<PaymentWebhookProcessingResult> nagadCallback(HttpServletRequest request) {
        return receive("NAGAD", queryPayload(request), null);
    }

    @PostMapping(value = "/nagad/callback", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive Nagad callback")
    public ApiResponse<PaymentWebhookProcessingResult> nagadCallback(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(value = PAYMENT_SIGNATURE_HEADER, required = false) String signature
    ) {
        return receive("NAGAD", payload(request, body, null), signature);
    }

    @GetMapping("/rocket/callback")
    @Operation(summary = "Receive Rocket callback")
    public ApiResponse<PaymentWebhookProcessingResult> rocketCallback(HttpServletRequest request) {
        return receive("ROCKET", queryPayload(request), null);
    }

    @PostMapping(value = "/rocket/callback", consumes = MediaType.ALL_VALUE)
    @Operation(summary = "Receive Rocket callback")
    public ApiResponse<PaymentWebhookProcessingResult> rocketCallback(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(value = PAYMENT_SIGNATURE_HEADER, required = false) String signature
    ) {
        return receive("ROCKET", payload(request, body, null), signature);
    }

    private ApiResponse<PaymentWebhookProcessingResult> receive(String providerCode, String payload, String signature) {
        return ApiResponse.success(paymentWebhookService.receiveWebhook(new PaymentWebhookCommand(providerCode, payload, signature)));
    }

    private String withStatus(HttpServletRequest request, String status) {
        return payload(request, null, status);
    }

    private String payload(HttpServletRequest request, String body, String fallbackStatus) {
        String normalizedBody = body == null ? "" : body.trim();
        if (!normalizedBody.isBlank()) {
            if (fallbackStatus == null || normalizedBody.contains("status=")) {
                return normalizedBody;
            }
            return normalizedBody + "&status=" + fallbackStatus;
        }
        String queryPayload = queryPayload(request);
        if (fallbackStatus == null || queryPayload.contains("status=")) {
            return queryPayload;
        }
        return queryPayload.isBlank() ? "status=" + fallbackStatus : queryPayload + "&status=" + fallbackStatus;
    }

    private String queryPayload(HttpServletRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            for (String value : entry.getValue()) {
                params.add(entry.getKey(), value);
            }
        }
        String query = UriComponentsBuilder.newInstance().queryParams(params).build().getQuery();
        return query == null ? "" : query;
    }
}
