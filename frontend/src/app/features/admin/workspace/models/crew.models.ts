import { Permission, UserRole } from '@app/features/auth/models/user.models';

export type CrewMemberStatus = 'ACTIVE' | 'INVITED' | 'SUSPENDED' | 'REVOKED';
export type CrewPermission = Extract<
  Permission,
  | 'WORKSPACE_VIEW'
  | 'CREATIVE_GENERATE'
  | 'CREATIVE_EDIT'
  | 'CREATIVE_DOWNLOAD'
  | 'CREATIVE_SUBMIT'
>;

export interface CrewMember {
  readonly id: string;
  readonly userId: string;
  readonly workspaceId: string;
  readonly firstName: string | null;
  readonly lastName: string | null;
  readonly email: string;
  readonly phone: string | null;
  readonly role: UserRole;
  readonly status: CrewMemberStatus;
  readonly permissions: readonly CrewPermission[];
  readonly joinedAt: string;
  readonly invitedByUserId: string | null;
  readonly lastLoginAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CrewInvitation {
  readonly invitationToken: string;
  readonly workspaceId: string;
  readonly email: string;
  readonly role: UserRole;
  readonly permissions: readonly CrewPermission[];
  readonly expiresAt: string;
  readonly status: string;
}

export interface InviteCrewPayload {
  readonly email: string;
  readonly role: 'CREW';
  readonly permissions: readonly CrewPermission[];
}

export interface UpdateCrewMemberPayload {
  readonly permissions: readonly CrewPermission[];
  readonly status: Extract<CrewMemberStatus, 'ACTIVE' | 'SUSPENDED'>;
}

export interface CrewPermissionOption {
  readonly permission: CrewPermission;
  readonly label: string;
  readonly description: string;
}

export const CREW_PERMISSION_OPTIONS: readonly CrewPermissionOption[] = [
  {
    permission: 'WORKSPACE_VIEW',
    label: 'Workspace View',
    description: 'Open workspace summaries and brand context in read-only mode.',
  },
  {
    permission: 'CREATIVE_GENERATE',
    label: 'Creative Generate',
    description: 'Generate new platform creatives when the future module is enabled.',
  },
  {
    permission: 'CREATIVE_EDIT',
    label: 'Creative Edit',
    description: 'Refine draft creatives, copy, and layout variants.',
  },
  {
    permission: 'CREATIVE_DOWNLOAD',
    label: 'Creative Download',
    description: 'Download approved creative files from the workspace.',
  },
  {
    permission: 'CREATIVE_SUBMIT',
    label: 'Creative Submit',
    description: 'Submit finished creative work for review or publishing.',
  },
];

export const CREW_STATUS_LABELS: Record<CrewMemberStatus, string> = {
  ACTIVE: 'Active',
  INVITED: 'Invited',
  SUSPENDED: 'Suspended',
  REVOKED: 'Removed',
};

export function normalizeCrewPermissions(
  permissions: readonly CrewPermission[],
): readonly CrewPermission[] {
  const unique = new Set<CrewPermission>(permissions);
  unique.add('WORKSPACE_VIEW');

  return CREW_PERMISSION_OPTIONS.map((option) => option.permission).filter((permission) =>
    unique.has(permission),
  );
}

export function crewPermissionSummary(permissions: readonly CrewPermission[]): string {
  if (permissions.length <= 1) {
    return 'Read-only workspace access';
  }

  const actionLabels = CREW_PERMISSION_OPTIONS.filter(
    (option) => option.permission !== 'WORKSPACE_VIEW' && permissions.includes(option.permission),
  ).map((option) => option.label);

  return actionLabels.join(', ');
}
