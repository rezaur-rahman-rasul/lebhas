export type {
  CreditLedgerItemView,
  GenerationCreditPreviewView,
  WorkspaceCreditAccountView,
} from '@app/features/master/provider-settings/models/provider-credit-exchange.models';

export interface CreditLedgerFilters {
  readonly transactionType?: string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly search?: string | null;
}

export const CREDIT_TRANSACTION_LABELS: Readonly<Record<string, string>> = {
  FREE_SIGNUP_CREDIT_GRANTED: 'Free signup credits',
  CREDIT_PURCHASED: 'Purchased credits',
  CREDIT_RESERVED: 'Reserved for generation',
  CREDIT_FINALIZED: 'Used for generation',
  CREDIT_REFUNDED: 'Refunded after failed generation',
  MANUAL_ADJUSTMENT: 'Manual adjustment',
  RESERVE: 'Reserved for generation',
  FINALIZE: 'Used for generation',
  REFUND: 'Refunded after failed generation',
};

export function creditTransactionLabel(type: string | null | undefined): string {
  if (!type) {
    return 'Credit update';
  }
  return CREDIT_TRANSACTION_LABELS[type] ?? type.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

