import { Permission, UserRole } from '@app/features/auth/models/user.models';

export interface WorkspaceSummary {
  readonly id: string;
  readonly name: string;
  readonly slug: string;
  readonly logoUrl: string | null;
  readonly status: string;
  readonly language: string;
  readonly timezone: string;
  readonly ownerId: string;
  readonly currentUserRole: UserRole;
  readonly currentUserPermissions: readonly Permission[];
  readonly createdAt: string;
  readonly updatedAt: string;
}

