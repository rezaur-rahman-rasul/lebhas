import { UserProfileView } from '../models/profile.models';

const allowedImageTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);
const maxProfileImageBytes = 5 * 1024 * 1024;

interface ResettableForm {
  reset(value?: unknown): void;
}

export function getInitials(profile: Pick<UserProfileView, 'firstName' | 'lastName' | 'displayName'> | null): string {
  if (!profile) {
    return 'U';
  }

  const source = [profile.firstName, profile.lastName].filter(Boolean).join(' ') || profile.displayName;
  const initials = source
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');

  return initials || 'U';
}

export function safeProfileImageUrl(profile: Pick<UserProfileView, 'profileImageUrl'> | null): string | null {
  const url = profile?.profileImageUrl?.trim();
  if (!url) {
    return null;
  }

  if (url.startsWith('https://') || url.startsWith('/') || url.startsWith('blob:')) {
    return url;
  }

  return null;
}

export function safeProfileDisplayName(
  value: string | null | undefined,
  fallback = 'Workspace member',
): string {
  const trimmed = value?.trim();
  if (!trimmed || looksLikeTechnicalIdentifier(trimmed) || looksSensitive(trimmed)) {
    return fallback;
  }

  return trimmed;
}

export function safeProfileAvatarUrl(value: string | null | undefined): string | null {
  const url = value?.trim();
  if (!url || looksSensitive(url)) {
    return null;
  }

  return url.startsWith('https://') || url.startsWith('/') || url.startsWith('blob:') ? url : null;
}

export function getInitialsFromDisplayName(value: string | null | undefined): string {
  const name = safeProfileDisplayName(value, 'User');
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');

  return initials || 'U';
}

export function maskIpAddress(value: string | null | undefined): string {
  if (!value) {
    return 'Unknown location';
  }

  if (value.includes(':')) {
    const parts = value.split(':').filter(Boolean);
    return parts.length > 1 ? `${parts.slice(0, 2).join(':')}:...` : 'Masked address';
  }

  const parts = value.split('.');
  if (parts.length === 4) {
    return `${parts[0]}.${parts[1]}.***.***`;
  }

  return 'Masked address';
}

export function summarizeUserAgent(value: string | null | undefined): string {
  if (!value) {
    return 'Unknown device';
  }

  const lower = value.toLowerCase();
  const browser = lower.includes('edg/')
    ? 'Edge'
    : lower.includes('chrome/')
      ? 'Chrome'
      : lower.includes('firefox/')
        ? 'Firefox'
        : lower.includes('safari/')
          ? 'Safari'
          : 'Browser';
  const device = lower.includes('mobile') ? 'mobile device' : 'desktop device';

  return `${browser} on ${device}`;
}

export function clearPasswordFormState(form: ResettableForm | null | undefined): void {
  form?.reset({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
}

export function isAllowedProfileImageType(file: Pick<File, 'type'>): boolean {
  return allowedImageTypes.has(file.type);
}

export function isAllowedProfileImageSize(file: Pick<File, 'size'>): boolean {
  return file.size > 0 && file.size <= maxProfileImageBytes;
}

function looksLikeTechnicalIdentifier(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

function looksSensitive(value: string): boolean {
  return /(objectkey|object_key|token|refresh|password|secret|credential|api[-_]?key|webhook|jwt|session)/i.test(value);
}
