import { Permission, UserRole } from '@app/features/auth/models/user.models';

export interface FeatureAvailability {
  readonly enabled: boolean;
  readonly reason?: string | null;
}

export interface FeatureLimit {
  readonly used?: number | null;
  readonly limit?: number | null;
  readonly remaining?: number | null;
  readonly unit?: string | null;
  readonly message?: string | null;
}

export interface PlanFeaturePolicy {
  readonly features?: Readonly<Record<string, FeatureAvailability>>;
  readonly limits?: Readonly<Record<string, FeatureLimit>>;
  readonly generatedVersionLimit?: number | null;
  readonly approvalAvailable?: boolean | null;
  readonly shareAvailable?: boolean | null;
}

export interface WorkspaceSubscription {
  readonly id?: string | null;
  readonly status?: string | null;
  readonly planId?: string | null;
  readonly planName?: string | null;
  readonly billingCycle?: string | null;
  readonly currentPeriodEnd?: string | null;
  readonly trialEndsAt?: string | null;
}

export interface WorkspaceUsageSummary {
  readonly creditsRemaining?: number | null;
  readonly storageRemainingBytes?: number | null;
  readonly limits?: Readonly<Record<string, FeatureLimit>>;
}

export interface WorkspaceContext {
  readonly workspace?: WorkspaceSummary | null;
  readonly subscription?: WorkspaceSubscription | null;
  readonly featurePolicy?: PlanFeaturePolicy | null;
  readonly usage?: WorkspaceUsageSummary | null;
  readonly featureToggles?: Readonly<Record<string, boolean>>;
}

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
  readonly subscription?: WorkspaceSubscription | null;
  readonly featurePolicy?: PlanFeaturePolicy | null;
  readonly usage?: WorkspaceUsageSummary | null;
  readonly featureToggles?: Readonly<Record<string, boolean>>;
  readonly createdAt: string;
  readonly updatedAt: string;
}

