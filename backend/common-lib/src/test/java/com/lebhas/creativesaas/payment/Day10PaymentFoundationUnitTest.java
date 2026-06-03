package com.lebhas.creativesaas.payment;

import com.lebhas.creativesaas.payment.application.InvoiceService;
import com.lebhas.creativesaas.payment.application.PaymentEventProducer;
import com.lebhas.creativesaas.payment.application.PaymentProviderMapper;
import com.lebhas.creativesaas.payment.application.PaymentSessionRequest;
import com.lebhas.creativesaas.payment.application.PaymentSessionResponse;
import com.lebhas.creativesaas.payment.application.PaymentTransactionService;
import com.lebhas.creativesaas.payment.cache.PaymentTransactionCacheService;
import com.lebhas.creativesaas.payment.domain.CreditPackage;
import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.InvoiceStatus;
import com.lebhas.creativesaas.payment.domain.InvoiceType;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.infrastructure.persistence.InvoiceRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentTransactionRepository;
import com.lebhas.creativesaas.payment.infrastructure.provider.ManualPaymentClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Day10PaymentFoundationUnitTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROVIDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID REFERENCE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void masterCreatesProviderEntity() {
        PaymentProvider provider = provider();

        assertThat(provider.getCode()).isEqualTo("MANUAL_PROVIDER");
        assertThat(provider.getProviderType()).isEqualTo(PaymentProviderType.MANUAL);
        assertThat(provider.isEnabled()).isTrue();
    }

    @Test
    void providerConfigurationViewMasksSecrets() {
        PaymentProviderConfiguration configuration = configuration();

        var view = new PaymentProviderMapper().toConfigurationView(configuration);

        assertThat(view.apiKeyConfigured()).isTrue();
        assertThat(view.secretConfigured()).isTrue();
        assertThat(view.webhookSecretConfigured()).isTrue();
        assertThat(view.toString()).doesNotContain("raw-api-key", "raw-secret", "raw-webhook-secret");
    }

    @Test
    void creditPackageIsDynamicAndNotNamedByPlanLogic() {
        CreditPackage creditPackage = CreditPackage.create(
                "Launch Credits",
                "launch_credits_250",
                250,
                25,
                new BigDecimal("19.99"),
                "usd",
                true,
                7);

        assertThat(creditPackage.getCode()).isEqualTo("LAUNCH_CREDITS_250");
        assertThat(creditPackage.getCredits()).isEqualTo(250);
        assertThat(creditPackage.getPrice()).isEqualByComparingTo("19.9900");
    }

    @Test
    void subscriptionPurchaseTransactionUsesBackendAmount() {
        PaymentTransaction transaction = transactionService().createTransaction(
                WORKSPACE_ID,
                USER_ID,
                PROVIDER_ID,
                PaymentPurpose.SUBSCRIPTION_PURCHASE,
                "SUBSCRIPTION_ORDER",
                REFERENCE_ID,
                new BigDecimal("49.99"),
                "usd");

        assertThat(transaction.getAmount()).isEqualByComparingTo("49.9900");
        assertThat(transaction.getPaymentPurpose()).isEqualTo(PaymentPurpose.SUBSCRIPTION_PURCHASE);
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.INITIATED);
    }

    @Test
    void creditPurchaseTransactionUsesBackendAmount() {
        PaymentTransaction transaction = transactionService().createTransaction(
                WORKSPACE_ID,
                USER_ID,
                PROVIDER_ID,
                PaymentPurpose.CREDIT_PURCHASE,
                "CREDIT_PURCHASE_ORDER",
                REFERENCE_ID,
                new BigDecimal("15.25"),
                "usd");

        assertThat(transaction.getAmount()).isEqualByComparingTo("15.2500");
        assertThat(transaction.getPaymentPurpose()).isEqualTo(PaymentPurpose.CREDIT_PURCHASE);
    }

    @Test
    void invoiceFoundationCreatedForSuccessfulPaymentFlow() {
        InvoiceRepository repository = mock(InvoiceRepository.class);
        when(repository.save(any(Invoice.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        InvoiceService service = new InvoiceService(repository, mock(PaymentEventProducer.class));

        Invoice invoice = service.issueCreditPurchaseInvoice(WORKSPACE_ID, REFERENCE_ID, new BigDecimal("29.99"), "usd");

        assertThat(invoice.getInvoiceType()).isEqualTo(InvoiceType.CREDIT_PURCHASE);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.getInvoiceNumber()).startsWith("INV-CRD-");
        verify(repository).save(any(Invoice.class));
    }

    @Test
    void foundationPaymentClientDoesNotCallRealGatewayOrExposeSecrets() {
        PaymentSessionResponse response = new ManualPaymentClient().createSession(provider(), configuration(), new PaymentSessionRequest(
                WORKSPACE_ID,
                USER_ID,
                PaymentPurpose.CREDIT_PURCHASE,
                new BigDecimal("10.00"),
                "USD",
                "CREDIT_PURCHASE_ORDER",
                REFERENCE_ID,
                PaymentEnvironmentType.SANDBOX,
                null,
                "idem-1",
                Map.of()));

        assertThat(response.status()).isEqualTo(PaymentTransactionStatus.PENDING);
        assertThat(response.redirectUrl()).isNull();
        assertThat(response.providerPayload().toString()).doesNotContain("encrypted-api-key", "encrypted-secret", "encrypted-webhook-secret");
    }

    @Test
    void paymentSourceDoesNotHardcodePackageNamesOrExposeSecretFieldsInViews() throws Exception {
        Pattern forbiddenPlanNames = Pattern.compile("(?i)\\b(free|basic|pro|enterprise)\\b");
        Pattern rawSecretView = Pattern.compile("PaymentProviderConfigurationView\\([^;]*(apiKey|secret|webhookSecret)\\)");

        assertThat(sourceFilesWithMatch(forbiddenPlanNames)).isEmpty();
        assertThat(sourceFilesWithMatch(rawSecretView)).isEmpty();
    }

    private PaymentTransactionService transactionService() {
        PaymentTransactionRepository repository = mock(PaymentTransactionRepository.class);
        when(repository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        return new PaymentTransactionService(
                repository,
                mock(PaymentTransactionCacheService.class),
                mock(PaymentEventProducer.class));
    }

    private PaymentProvider provider() {
        PaymentProvider provider = PaymentProvider.create(
                "Manual Provider",
                "manual_provider",
                PaymentProviderType.MANUAL,
                true,
                true,
                false,
                1);
        ReflectionTestUtils.setField(provider, "id", PROVIDER_ID);
        return provider;
    }

    private PaymentProviderConfiguration configuration() {
        PaymentProviderConfiguration configuration = PaymentProviderConfiguration.create(
                PROVIDER_ID,
                PaymentEnvironmentType.SANDBOX,
                "https://payments.example.test",
                "merchant-1",
                "encrypted-api-key",
                "encrypted-secret",
                "encrypted-webhook-secret",
                "https://app.example.test/success",
                "https://app.example.test/failure",
                "https://app.example.test/cancel",
                true);
        ReflectionTestUtils.setField(configuration, "id", UUID.randomUUID());
        return configuration;
    }

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    private List<String> sourceFilesWithMatch(Pattern pattern) throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/com/lebhas/creativesaas/payment"))) {
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
}
