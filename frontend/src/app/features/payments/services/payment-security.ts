const MASK = '••••';

export function maskSecret(value: string | null | undefined): string {
  if (!value) {
    return MASK;
  }

  if (value.length <= 4) {
    return MASK;
  }

  return `${MASK}${value.slice(-4)}`;
}

export function maskProviderTransactionId(value: string | null | undefined): string {
  if (!value) {
    return 'Not available';
  }

  if (value.length <= 8) {
    return maskSecret(value);
  }

  return `${value.slice(0, 4)}${MASK}${value.slice(-4)}`;
}

export function safeDisplaySecret(value: string | null | undefined): string {
  return maskSecret(value);
}
