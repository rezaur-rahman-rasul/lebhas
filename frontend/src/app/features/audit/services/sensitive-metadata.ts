const MASK = '****';
const SECRET_KEYS = [
  'apiKey',
  'authorization',
  'cardNumber',
  'jwt',
  'objectKey',
  'password',
  'paymentSecret',
  'profileImageObjectKey',
  'providerSecret',
  'r2ObjectKey',
  'secret',
  'sessionToken',
  'token',
  'webhookSecret',
];

export function maskSensitiveMetadata(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }

  try {
    const parsed = JSON.parse(value) as unknown;
    return JSON.stringify(maskObject(parsed));
  } catch {
    return maskText(value);
  }
}

export function summarizeIpAddress(value: string | null | undefined): string {
  if (!value) {
    return 'Not available';
  }

  const parts = value.split('.');
  if (parts.length === 4) {
    return `${parts[0]}.${parts[1]}.*.*`;
  }

  return value.length > 8 ? `${value.slice(0, 4)}...${value.slice(-4)}` : MASK;
}

export function summarizeUserAgent(value: string | null | undefined): string {
  if (!value) {
    return 'Not available';
  }

  const browser = value.match(/(Chrome|Firefox|Safari|Edge|OPR)\/?[\d.]*/i)?.[0];
  const platform = value.match(/\(([^)]+)\)/)?.[1]?.split(';')[0];
  return [browser, platform].filter(Boolean).join(' on ') || 'Browser details available';
}

export function safeMetadataPreview(value: string | null | undefined): string | null {
  const masked = maskSensitiveMetadata(value);
  if (!masked) {
    return null;
  }

  return masked.length > 240 ? `${masked.slice(0, 240)}...` : masked;
}

function maskObject(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(maskObject);
  }

  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>).map(([key, entry]) => [
        key,
        isSensitiveKey(key) ? MASK : maskObject(entry),
      ]),
    );
  }

  return typeof value === 'string' ? maskText(value) : value;
}

function maskText(value: string): string {
  return SECRET_KEYS.reduce(
    (current, key) =>
      current.replace(new RegExp(`("${key}"\\s*:\\s*")([^"]+)(")`, 'gi'), `$1${MASK}$3`),
    value,
  );
}

function isSensitiveKey(key: string): boolean {
  const normalized = key.toLowerCase();
  return SECRET_KEYS.some((secretKey) => normalized.includes(secretKey.toLowerCase()));
}
