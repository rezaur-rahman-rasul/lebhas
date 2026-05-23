import { Permission, UserRole } from '@app/features/auth/models/user.models';

export function hasRole(role: UserRole | null, expected: UserRole): boolean {
  return role === expected;
}

export function hasAnyRole(
  role: UserRole | null,
  expectedRoles: readonly UserRole[],
): boolean {
  return role !== null && expectedRoles.includes(role);
}

export function hasPermission(
  permissions: readonly Permission[],
  permission: Permission,
): boolean {
  return permissions.includes(permission);
}

export function defaultAuthenticatedRoute(): string {
  return '/dashboard';
}

export function roleBadgeTone(role: UserRole | null): 'brand' | 'blue' | 'red' | 'neutral' {
  switch (role) {
    case 'MASTER':
      return 'red';
    case 'ADMIN':
      return 'brand';
    case 'CREW':
      return 'blue';
    default:
      return 'neutral';
  }
}
