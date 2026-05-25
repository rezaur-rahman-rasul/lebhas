ALTER TABLE platform.payment_webhook_logs
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ;

ALTER TABLE platform.credit_ledger
    DROP CONSTRAINT IF EXISTS chk_credit_ledger_transaction_type;

ALTER TABLE platform.credit_ledger
    ADD CONSTRAINT chk_credit_ledger_transaction_type
        CHECK (transaction_type IN ('PURCHASE', 'RESERVE', 'FINALIZE', 'REFUND', 'MANUAL_ADJUSTMENT', 'SYSTEM_ADJUSTMENT', 'EXPIRY'));
