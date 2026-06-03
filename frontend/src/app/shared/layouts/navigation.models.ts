import { UserRole } from '@app/features/auth/models/user.models';

export interface NavigationItem {
  readonly label: string;
  readonly icon: string;
  readonly route: string;
  readonly group?: string;
  readonly description?: string;
  readonly requiresWorkspace?: boolean;
  readonly roles?: readonly UserRole[];
  readonly permissionKey?: NavigationPermissionKey;
  readonly exact?: boolean;
  readonly activeMatch?: 'exact' | 'prefix';
  readonly activePaths?: readonly string[];
}

export type NavigationPermissionKey =
  | 'canViewBrands'
  | 'canViewProducts'
  | 'canViewProjects'
  | 'canViewAiMonitoring'
  | 'canViewProviderMetrics'
  | 'canViewLayerAnalytics'
  | 'canViewWorkspaceAiUsage'
  | 'canViewQualityScores'
  | 'canViewAiFailures'
  | 'canViewUsageBilling'
  | 'canViewCreditLedger'
  | 'canViewDownloadUsage'
  | 'canViewShareUsage'
  | 'canViewMasterUsage'
  | 'canViewPlanUtilization'
  | 'canManagePaymentProviders'
  | 'canManageCreditPackages'
  | 'canPurchaseSubscription'
  | 'canPurchaseCredits'
  | 'canViewPayments'
  | 'canViewInvoices'
  | 'canViewNotifications'
  | 'canManageNotificationPreferences'
  | 'canViewActivityFeed'
  | 'canViewAuditLogs'
  | 'canViewMasterMonitoring'
  | 'canViewSystemHealth'
  | 'canViewMonitoringAlerts';

