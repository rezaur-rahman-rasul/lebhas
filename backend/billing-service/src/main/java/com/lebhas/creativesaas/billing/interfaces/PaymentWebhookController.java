package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.payment.application.PaymentWebhookService;
import com.lebhas.creativesaas.payment.application.dto.PaymentWebhookCommand;
import com.lebhas.creativesaas.payment.application.dto.PaymentWebhookProcessingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
@Tag(name = "Payment Webhooks")
public class PaymentWebhookController {

    private static final String PAYMENT_SIGNATURE_HEADER = "X-Payment-Signature";

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/{providerCode}")
    @Operation(summary = "Receive payment provider webhook")
    public ApiResponse<PaymentWebhookProcessingResult> receiveWebhook(
            @PathVariable String providerCode,
            @RequestBody String payload,
            @RequestHeader(value = PAYMENT_SIGNATURE_HEADER, required = false) String signature
    ) {
        return ApiResponse.success(paymentWebhookService.receiveWebhook(new PaymentWebhookCommand(
                providerCode,
                payload,
                signature
        )));
    }
}
