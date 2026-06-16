import { UserRole } from '@app/features/auth/models/user.models';

export function getDefaultRouteForRole(role: UserRole | string | null | undefined): string {
  switch (role) {
    case 'MASTER':
      return '/master/dashboard';
    case 'ADMIN':
    case 'CREW':
      return '/creative-generator';
    default:
      return '/dashboard';
  }
}

export function resolvePostLoginRedirect(
  role: UserRole | string | null | undefined,
  returnUrl?: string | null,
): string {
  const defaultRoute = getDefaultRouteForRole(role);
  const normalizedReturnUrl = normalizeReturnUrl(returnUrl);

  if (!normalizedReturnUrl) {
    return defaultRoute;
  }

  return isReturnUrlAllowedForRole(role, normalizedReturnUrl)
    ? normalizedReturnUrl
    : defaultRoute;
}

function normalizeReturnUrl(returnUrl?: string | null): string | null {
  if (!returnUrl) {
    return null;
  }

  const trimmed = returnUrl.trim();
  if (!trimmed || !trimmed.startsWith('/') || trimmed.startsWith('//')) {
    return null;
  }

  return trimmed;
}

function isReturnUrlAllowedForRole(
  role: UserRole | string | null | undefined,
  returnUrl: string,
): boolean {
  const path = returnUrl.split('?')[0]?.split('#')[0] ?? returnUrl;

  if (role === 'MASTER') {
    return path === '/audit-logs' || path.startsWith('/master/');
  }

  if (role === 'ADMIN' || role === 'CREW') {
    return path !== '/' && path !== '/master' && !path.startsWith('/master/');
  }

  return false;
}
