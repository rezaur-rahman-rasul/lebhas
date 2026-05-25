package com.lebhas.creativesaas.billing;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.payment.application.PaymentProviderResolver;
import com.lebhas.creativesaas.payment.application.PaymentSessionRequest;
import com.lebhas.creativesaas.payment.application.PaymentStatusSynchronizer;
import com.lebhas.creativesaas.payment.application.PaymentTransactionService;
import com.lebhas.creativesaas.payment.application.PaymentWebhookVerificationResult;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchasePaymentSessionView;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionPaymentSessionView;
import com.lebhas.creativesaas.payment.cache.PaymentSessionCacheService;
import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.CreditPackage;
import com.lebhas.creativesaas.payment.domain.CreditPurchaseOrder;
import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.InvoiceStatus;
import com.lebhas.creativesaas.payment.domain.InvoiceType;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookLog;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;
import com.lebhas.creativesaas.payment.domain.SubscriptionOrder;
import com.lebhas.creativesaas.payment.infrastructure.persistence.CreditPackageRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.CreditPurchaseOrderRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.InvoiceRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderConfigurationRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentTransactionRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentWebhookLogRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.SubscriptionOrderRepository;
import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;
import com.lebhas.creativesaas.usage.infrastructure.persistence.CreditLedgerRepository;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Day9PaymentBackendIntegrationTest extends AbstractBillingPricingIntegrationTest {

    @Autowired
    PaymentProviderRepository paymentProviderRepository;

    @Autowired
    PaymentProviderConfigurationRepository paymentProviderConfigurationRepository;

    @Autowired
    PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    SubscriptionOrderRepository subscriptionOrderRepository;

    @Autowired
    CreditPackageRepository creditPackageRepository;

    @Autowired
    CreditPurchaseOrderRepository creditPurchaseOrderRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    PaymentWebhookLogRepository paymentWebhookLogRepository;

    @Autowired
    PaymentProviderResolver paymentProviderResolver;

    @Autowired
    PaymentSessionCacheService paymentSessionCacheService;

    @Autowired
    PaymentStatusSynchronizer paymentStatusSynchronizer;

    @Autowired
    PaymentTransactionService paymentTransactionService;

    @Autowired
    CreditLedgerRepository creditLedgerRepository;

    @Autowired
    WorkspaceSubscriptionRepository paymentWorkspaceSubscriptionRepository;

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUpPaymentTests() {
        creditLedgerRepository.deleteAll();
        invoiceRepository.deleteAll();
        creditPurchaseOrderRepository.deleteAll();
        subscriptionOrderRepository.deleteAll();
        paymentWebhookLogRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        paymentProviderConfigurationRepository.deleteAll();
        paymentProviderRepository.deleteAll();
        creditPackageRepository.deleteAll();
        reset(domainEventPublisher);
    }

    @Test
    void paymentProviderPersistsCorrectly() {
        PaymentProvider provider = saveProvider("MANUAL_PROVIDER", PaymentProviderType.MANUAL, true, true, false, 5);

        PaymentProvider reloaded = paymentProviderRepository.findByCode("MANUAL_PROVIDER").orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(provider.getId());
        assertThat(reloaded.getProviderType()).isEqualTo(PaymentProviderType.MANUAL);
        assertThat(reloaded.isEnabled()).isTrue();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void paymentProviderConfigurationPersistsCorrectly() {
        PaymentProvider provider = saveProvider("CONFIG_PROVIDER", PaymentProviderType.MANUAL, true, true, true, 1);
        PaymentProviderConfiguration configuration = saveConfiguration(provider, PaymentEnvironmentType.SANDBOX, true);

        PaymentProviderConfiguration reloaded = paymentProviderConfigurationRepository
                .findByProviderIdAndEnvironmentTypeAndActiveTrue(provider.getId(), PaymentEnvironmentType.SANDBOX)
                .orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(configuration.getId());
        assertThat(reloaded.getMerchantId()).isEqualTo("merchant-CONFIG_PROVIDER");
        assertThat(reloaded.getEncryptedApiKey()).isNotBlank();
    }

    @Test
    void paymentTransactionPersistsCorrectly() {
        PaymentProvider provider = configuredProvider("TRANSACTION_PROVIDER", 1);
        PaymentTransaction saved = saveTransaction(provider, PaymentPurpose.SUBSCRIPTION_PURCHASE, UUID.randomUUID(), new BigDecimal("19.9900"));

        PaymentTransaction reloaded = paymentTransactionRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getWorkspaceId()).isEqualTo(workspaceOne.getId());
        assertThat(reloaded.getAmount()).isEqualByComparingTo("19.9900");
        assertThat(reloaded.getStatus()).isEqualTo(PaymentTransactionStatus.INITIATED);
    }

    @Test
    void subscriptionOrderPersistsCorrectly() {
        PricingPlan plan = createCustomPlan("sub-order", true);
        SubscriptionOrder saved = subscriptionOrderRepository.save(SubscriptionOrder.create(
                workspaceOne.getId(), plan.getId(), adminUser.getId(), BillingCycle.MONTHLY, new BigDecimal("12.5000"), "USD", null, null));

        SubscriptionOrder reloaded = subscriptionOrderRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getPricingPlanId()).isEqualTo(plan.getId());
        assertThat(reloaded.getStatus()).isEqualTo(PaymentOrderStatus.CREATED);
    }

    @Test
    void creditPackagePersistsCorrectly() {
        CreditPackage saved = saveCreditPackage("CREDITS_100", true, new BigDecimal("10.0000"));

        CreditPackage reloaded = creditPackageRepository.findByCode("CREDITS_100").orElseThrow();

        assertThat(reloaded.getCredits()).isEqualTo(100);
        assertThat(reloaded.getBonusCredits()).isEqualTo(10);
        assertThat(reloaded.isActive()).isTrue();
    }

    @Test
    void creditPurchaseOrderPersistsCorrectly() {
        CreditPackage creditPackage = saveCreditPackage("ORDER_CREDITS", true, new BigDecimal("15.0000"));
        CreditPurchaseOrder saved = creditPurchaseOrderRepository.save(CreditPurchaseOrder.create(
                workspaceOne.getId(), creditPackage.getId(), adminUser.getId(), 110, creditPackage.getPrice(), "USD"));

        CreditPurchaseOrder reloaded = creditPurchaseOrderRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getCreditPackageId()).isEqualTo(creditPackage.getId());
        assertThat(reloaded.getCredits()).isEqualTo(110);
    }

    @Test
    void invoicePersistsCorrectly() {
        PaymentProvider provider = configuredProvider("INVOICE_PROVIDER", 1);
        PaymentTransaction transaction = saveTransaction(provider, PaymentPurpose.SUBSCRIPTION_PURCHASE, UUID.randomUUID(), BigDecimal.TEN);
        Invoice saved = invoiceRepository.save(Invoice.create(
                workspaceOne.getId(), transaction.getId(), "INV-TEST-1", InvoiceType.SUBSCRIPTION, BigDecimal.TEN, "USD", InvoiceStatus.ISSUED, Instant.now(), null));

        Invoice reloaded = invoiceRepository.findByInvoiceNumber("INV-TEST-1").orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(saved.getId());
        assertThat(reloaded.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    void paymentWebhookLogPersistsCorrectly() {
        PaymentProvider provider = configuredProvider("WEBHOOK_LOG_PROVIDER", 1);
        PaymentWebhookLog saved = paymentWebhookLogRepository.save(PaymentWebhookLog.create(
                provider.getId(), "provider-tx-1", "PAYMENT_SUCCESS", "{}", "hash-1", PaymentWebhookVerificationStatus.PENDING, false, null));

        PaymentWebhookLog reloaded = paymentWebhookLogRepository.findByProviderIdAndSignatureHash(provider.getId(), "hash-1").orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(saved.getId());
        assertThat(reloaded.getProviderTransactionId()).isEqualTo("provider-tx-1");
    }

    @Test
    void masterCanManagePaymentProviders() throws Exception {
        mockMvc.perform(post("/api/v1/master/payment-providers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "API Manual",
                                  "code": "API_MANUAL",
                                  "providerType": "MANUAL",
                                  "enabled": true,
                                  "sandboxEnabled": true,
                                  "liveEnabled": false,
                                  "priority": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("API_MANUAL"));
    }

    @Test
    void nonMasterCannotManagePaymentProviders() throws Exception {
        mockMvc.perform(post("/api/v1/master/payment-providers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Forbidden Manual",
                                  "code": "FORBIDDEN_MANUAL",
                                  "providerType": "MANUAL",
                                  "enabled": true,
                                  "sandboxEnabled": true,
                                  "liveEnabled": false,
                                  "priority": 2
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void masterCanManageCreditPackages() throws Exception {
        mockMvc.perform(post("/api/v1/master/credit-packages")
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "API Credits",
                                  "code": "API_CREDITS",
                                  "credits": 500,
                                  "bonusCredits": 50,
                                  "price": 25.0,
                                  "currency": "USD",
                                  "active": true,
                                  "sortOrder": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("API_CREDITS"));
    }

    @Test
    void publicCreditPackageApiReturnsActivePackagesOnly() throws Exception {
        CreditPackage active = saveCreditPackage("PUBLIC_ACTIVE", true, new BigDecimal("20.0000"));
        CreditPackage inactive = saveCreditPackage("PUBLIC_INACTIVE", false, new BigDecimal("30.0000"));

        MvcResult result = mockMvc.perform(get("/api/v1/credit-packages/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        assertThat(json(result).at("/data").findValuesAsText("code"))
                .contains(active.getCode())
                .doesNotContain(inactive.getCode());
    }

    @Test
    void subscriptionPurchaseCalculatesAmountServerSide() throws Exception {
        configuredProvider("SUBSCRIPTION_PROVIDER", 1);
        PricingPlan plan = createCustomPlan("server-amount", true);

        SubscriptionPaymentSessionView view = purchaseSubscription(plan, BillingCycle.YEARLY);

        PaymentTransaction transaction = paymentTransactionRepository.findById(view.paymentTransactionId()).orElseThrow();
        assertThat(transaction.getAmount()).isEqualByComparingTo(plan.getYearlyPrice());
    }

    @Test
    void creditPurchaseCalculatesAmountServerSide() throws Exception {
        configuredProvider("CREDIT_PROVIDER", 1);
        CreditPackage creditPackage = saveCreditPackage("SERVER_CREDITS", true, new BigDecimal("35.0000"));

        CreditPurchasePaymentSessionView view = purchaseCredits(creditPackage);

        PaymentTransaction transaction = paymentTransactionRepository.findById(view.paymentTransactionId()).orElseThrow();
        assertThat(transaction.getAmount()).isEqualByComparingTo(creditPackage.getPrice());
        assertThat(invoiceRepository.findByPaymentTransactionId(transaction.getId()))
                .isPresent()
                .get()
                .extracting(Invoice::getInvoiceType)
                .isEqualTo(InvoiceType.CREDIT_PURCHASE);
    }

    @Test
    void paymentProviderResolvesDynamically() {
        PaymentProvider slow = configuredProvider("SLOW_PROVIDER", 20);
        PaymentProvider fast = configuredProvider("FAST_PROVIDER", 1);

        PaymentProviderResolver.ResolvedPaymentProvider resolved = paymentProviderResolver.resolve(new PaymentSessionRequest(
                workspaceOne.getId(), adminUser.getId(), PaymentPurpose.SUBSCRIPTION_PURCHASE, BigDecimal.TEN, "USD",
                "subscription_order", UUID.randomUUID(), PaymentEnvironmentType.SANDBOX, null, "probe", Map.of()));

        assertThat(resolved.provider().getId()).isEqualTo(fast.getId());
        assertThat(resolved.provider().getId()).isNotEqualTo(slow.getId());
    }

    @Test
    void paymentSessionCachedInRedis() throws Exception {
        configuredProvider("CACHE_PROVIDER", 1);
        PricingPlan plan = createCustomPlan("cache-session", true);

        SubscriptionPaymentSessionView view = purchaseSubscription(plan, BillingCycle.MONTHLY);

        assertThat(paymentSessionCacheService.get(view.paymentTransactionId())).isPresent();
    }

    @Test
    void webhookLogCreated() throws Exception {
        configuredProvider("WEBHOOK_PROVIDER", 1);

        mockMvc.perform(post("/api/v1/payments/webhooks/WEBHOOK_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payment-Signature", "signature")
                        .content("{\"event\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(paymentWebhookLogRepository.findAll()).hasSize(1);
    }

    @Test
    void webhookSignatureVerificationFoundationWorks() throws Exception {
        configuredProvider("SIGNATURE_PROVIDER", 1);

        MvcResult result = mockMvc.perform(post("/api/v1/payments/webhooks/SIGNATURE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payment-Signature", "signature")
                        .content("{\"event\":\"test\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(json(result).at("/data/verificationStatus").asText()).isIn("PENDING", "FAILED");
    }

    @Test
    void webhookProcessingIsIdempotent() throws Exception {
        configuredProvider("IDEMPOTENT_PROVIDER", 1);
        String payload = "{\"event\":\"same\"}";

        mockMvc.perform(post("/api/v1/payments/webhooks/IDEMPOTENT_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payment-Signature", "same-signature")
                        .content(payload))
                .andExpect(status().isOk());
        MvcResult duplicate = mockMvc.perform(post("/api/v1/payments/webhooks/IDEMPOTENT_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payment-Signature", "same-signature")
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(paymentWebhookLogRepository.findAll()).hasSize(1);
        assertThat(json(duplicate).at("/data/duplicate").asBoolean()).isTrue();
    }

    @Test
    void successfulSubscriptionPaymentActivatesSubscription() throws Exception {
        configuredProvider("SUCCESS_SUB_PROVIDER", 1);
        PricingPlan plan = createCustomPlan("success-sub", true);
        SubscriptionPaymentSessionView view = purchaseSubscription(plan, BillingCycle.MONTHLY);
        PaymentTransaction transaction = paymentTransactionRepository.findById(view.paymentTransactionId()).orElseThrow();

        paymentStatusSynchronizer.synchronize(transaction, successResult("provider-success-sub"));

        assertThat(paymentWorkspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceOne.getId()))
                .isPresent()
                .get()
                .extracting(subscription -> subscription.getStatus())
                .isEqualTo(WorkspaceSubscriptionStatus.ACTIVE);
    }

    @Test
    void successfulCreditPaymentCreatesCreditLedgerEntry() throws Exception {
        configuredProvider("SUCCESS_CREDIT_PROVIDER", 1);
        CreditPackage creditPackage = saveCreditPackage("SUCCESS_CREDITS", true, new BigDecimal("40.0000"));
        CreditPurchasePaymentSessionView view = purchaseCredits(creditPackage);
        PaymentTransaction transaction = paymentTransactionRepository.findById(view.paymentTransactionId()).orElseThrow();

        paymentStatusSynchronizer.synchronize(transaction, successResult("provider-success-credit"));

        assertThat(creditLedgerRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceOne.getId()))
                .anySatisfy(ledger -> {
                    assertThat(ledger.getTransactionType()).isEqualTo(CreditLedgerTransactionType.PURCHASE);
                    assertThat(ledger.getCreditsAmount()).isEqualByComparingTo("110.0000");
                });
    }

    @Test
    void failedPaymentDoesNotActivateSubscription() throws Exception {
        configuredProvider("FAILED_SUB_PROVIDER", 1);
        PricingPlan plan = createCustomPlan("failed-sub", true);
        SubscriptionPaymentSessionView view = purchaseSubscription(plan, BillingCycle.MONTHLY);
        PaymentTransaction transaction = paymentTransactionRepository.findById(view.paymentTransactionId()).orElseThrow();

        paymentStatusSynchronizer.synchronize(transaction, failedResult());

        assertThat(paymentWorkspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceOne.getId())).isEmpty();
    }

    @Test
    void failedPaymentDoesNotAddCredits() throws Exception {
        configuredProvider("FAILED_CREDIT_PROVIDER", 1);
        CreditPackage creditPackage = saveCreditPackage("FAILED_CREDITS", true, new BigDecimal("44.0000"));
        CreditPurchasePaymentSessionView view = purchaseCredits(creditPackage);
        PaymentTransaction transaction = paymentTransactionRepository.findById(view.paymentTransactionId()).orElseThrow();

        paymentStatusSynchronizer.synchronize(transaction, failedResult());

        assertThat(creditLedgerRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceOne.getId())).isEmpty();
    }

    @Test
    void invoiceCreatedAfterPaymentTransaction() throws Exception {
        configuredProvider("INVOICE_FLOW_PROVIDER", 1);
        PricingPlan plan = createCustomPlan("invoice-flow", true);

        SubscriptionPaymentSessionView view = purchaseSubscription(plan, BillingCycle.MONTHLY);

        assertThat(invoiceRepository.findByPaymentTransactionId(view.paymentTransactionId())).isPresent();
    }

    @Test
    void kafkaPaymentTransactionInitiatedEventPublished() {
        PaymentProvider provider = configuredProvider("KAFKA_INIT_PROVIDER", 1);
        UUID referenceId = UUID.randomUUID();

        saveTransaction(provider, PaymentPurpose.SUBSCRIPTION_PURCHASE, referenceId, BigDecimal.TEN);

        verify(domainEventPublisher).publish(eq(KafkaTopicConstants.PAYMENT_TRANSACTION_INITIATED), any(BaseDomainEvent.class));
    }

    @Test
    void kafkaPaymentTransactionSucceededEventPublished() throws Exception {
        configuredProvider("KAFKA_SUCCESS_PROVIDER", 1);
        PricingPlan plan = createCustomPlan("kafka-success", true);
        SubscriptionPaymentSessionView view = purchaseSubscription(plan, BillingCycle.MONTHLY);
        PaymentTransaction transaction = paymentTransactionRepository.findById(view.paymentTransactionId()).orElseThrow();

        paymentStatusSynchronizer.synchronize(transaction, successResult("provider-kafka-success"));

        verify(domainEventPublisher).publish(eq(KafkaTopicConstants.PAYMENT_TRANSACTION_SUCCEEDED), any(BaseDomainEvent.class));
    }

    @Test
    void workspaceIsolationEnforced() throws Exception {
        WorkspaceEntityFixture other = createOtherWorkspace();

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/payments/transactions", other.workspaceId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void noHardcodedProviderSelection() throws Exception {
        Pattern forbidden = Pattern.compile("preferredProviderCode\\s*==|providerCode\\s*==|STRIPE\\s*\\)|BKASH\\s*\\)|SSLCOMMERZ\\s*\\)");

        assertThat(javaFilesContaining(Path.of("..", "common-lib", "src", "main", "java", "com", "lebhas", "creativesaas", "payment"), forbidden))
                .isEmpty();
    }

    @Test
    void noHardcodedPricingPlanLogic() throws Exception {
        Pattern forbidden = Pattern.compile("(?i)\\b(free|basic|pro|enterprise)\\b");

        assertThat(javaFilesContaining(Path.of("..", "common-lib", "src", "main", "java", "com", "lebhas", "creativesaas", "payment"), forbidden))
                .isEmpty();
    }

    @Test
    void standardApiResponseFormatWorks() {
        ApiResponse<String> response = ApiResponse.success("Payment OK", "payload");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Payment OK");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.errors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    private PaymentProvider configuredProvider(String code, int priority) {
        PaymentProvider provider = saveProvider(code, PaymentProviderType.MANUAL, true, true, false, priority);
        saveConfiguration(provider, PaymentEnvironmentType.SANDBOX, true);
        return provider;
    }

    private PaymentProvider saveProvider(String code, PaymentProviderType type, boolean enabled, boolean sandbox, boolean live, int priority) {
        return paymentProviderRepository.save(PaymentProvider.create(
                "Provider " + code,
                code,
                type,
                enabled,
                sandbox,
                live,
                priority));
    }

    private PaymentProviderConfiguration saveConfiguration(PaymentProvider provider, PaymentEnvironmentType environment, boolean active) {
        return paymentProviderConfigurationRepository.save(PaymentProviderConfiguration.create(
                provider.getId(),
                environment,
                "https://payments.example.test",
                "merchant-" + provider.getCode(),
                "encrypted-api-key",
                "encrypted-secret",
                "encrypted-webhook-secret",
                "https://app.example.test/success",
                "https://app.example.test/failure",
                "https://app.example.test/cancel",
                active));
    }

    private CreditPackage saveCreditPackage(String code, boolean active, BigDecimal price) {
        return creditPackageRepository.save(CreditPackage.create(
                "Package " + code,
                code,
                100,
                10,
                price,
                "USD",
                active,
                10));
    }

    private PaymentTransaction saveTransaction(PaymentProvider provider, PaymentPurpose purpose, UUID referenceId, BigDecimal amount) {
        return paymentTransactionService.createTransaction(
                workspaceOne.getId(),
                adminUser.getId(),
                provider.getId(),
                purpose,
                purpose == PaymentPurpose.CREDIT_PURCHASE ? "credit_purchase_order" : "subscription_order",
                referenceId,
                amount,
                "USD");
    }

    private SubscriptionPaymentSessionView purchaseSubscription(PricingPlan plan, BillingCycle billingCycle) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/subscriptions/purchase", workspaceOne.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pricingPlanId": "%s",
                                  "billingCycle": "%s",
                                  "environmentType": "SANDBOX"
                                }
                                """.formatted(plan.getId(), billingCycle.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return objectMapper.treeToValue(json(result).at("/data"), SubscriptionPaymentSessionView.class);
    }

    private CreditPurchasePaymentSessionView purchaseCredits(CreditPackage creditPackage) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/credits/purchase", workspaceOne.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "creditPackageId": "%s",
                                  "environmentType": "SANDBOX"
                                }
                                """.formatted(creditPackage.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return objectMapper.treeToValue(json(result).at("/data"), CreditPurchasePaymentSessionView.class);
    }

    private PaymentWebhookVerificationResult successResult(String providerTransactionId) {
        return new PaymentWebhookVerificationResult(
                true,
                PaymentWebhookVerificationStatus.VERIFIED,
                providerTransactionId,
                "PAYMENT_SUCCESS",
                PaymentTransactionStatus.SUCCESS,
                null,
                Map.of());
    }

    private PaymentWebhookVerificationResult failedResult() {
        return new PaymentWebhookVerificationResult(
                true,
                PaymentWebhookVerificationStatus.VERIFIED,
                "provider-failed-" + UUID.randomUUID(),
                "PAYMENT_FAILED",
                PaymentTransactionStatus.FAILED,
                "failed by provider",
                Map.of());
    }

    private WorkspaceEntityFixture createOtherWorkspace() {
        com.lebhas.creativesaas.workspace.domain.WorkspaceEntity workspace = workspaceRepository.save(
                com.lebhas.creativesaas.workspace.domain.WorkspaceEntity.create(
                        "Other Workspace",
                        "other-" + UUID.randomUUID(),
                        null,
                        null,
                        "Retail",
                        "Asia/Dhaka",
                        com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage.ENGLISH,
                        "USD",
                        "US",
                        masterUser.getId()));
        return new WorkspaceEntityFixture(workspace.getId());
    }

    private java.util.List<String> javaFilesContaining(Path root, Pattern pattern) throws Exception {
        try (var files = Files.walk(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, pattern))
                    .map(Path::toString)
                    .toList();
        }
    }

    private boolean contains(Path path, Pattern pattern) {
        try {
            return pattern.matcher(Files.readString(path, StandardCharsets.UTF_8)).find();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to inspect " + path, exception);
        }
    }

    private record WorkspaceEntityFixture(UUID workspaceId) {
    }
}
