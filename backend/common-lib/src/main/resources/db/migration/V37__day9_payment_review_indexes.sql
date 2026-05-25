CREATE UNIQUE INDEX IF NOT EXISTS uk_invoices_payment_transaction
    ON platform.invoices (payment_transaction_id);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_provider_transaction_processed
    ON platform.payment_webhook_logs (provider_id, provider_transaction_id, processed)
    WHERE provider_transaction_id IS NOT NULL;
