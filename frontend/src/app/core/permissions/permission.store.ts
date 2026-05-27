import { Injectable, computed, inject } from '@angular/core';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import {
  canEnhancePrompt,
  canManageTemplates,
  canUsePromptBuilder,
  canViewPromptHistory,
  canViewPromptTemplates,
  createPromptPermissionContext,
} from '@app/core/permissions/prompt.permissions';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { Permission } from '@app/features/auth/models/user.models';

interface PermissionCheckOptions {
  readonly feature?: string;
  readonly requireActiveSubscription?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PermissionStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly workspace = inject(WorkspaceStore);

  readonly role = this.auth.currentRole;
  readonly permissions = this.auth.permissions;
  readonly subscription = this.workspace.subscription;
  readonly featurePolicy = this.workspace.featurePolicy;

  private readonly promptPermissionContext = computed(() =>
    createPromptPermissionContext(this.role(), this.permissions()),
  );

  readonly hasActiveSubscription = computed(() => {
    const status = this.subscription()?.status?.toLowerCase();
    return !status || ['active', 'trialing'].includes(status);
  });

  readonly canViewBrands = computed(() => this.has('BRAND_VIEW', { feature: 'brands' }));
  readonly canManageBrands = computed(() => this.has('BRAND_MANAGE', { feature: 'brands.manage' }));
  readonly canViewProducts = computed(() => this.has('PRODUCT_VIEW', { feature: 'products' }));
  readonly canManageProducts = computed(() =>
    this.has('PRODUCT_MANAGE', { feature: 'products.manage' }),
  );
  readonly canViewProjects = computed(() => this.has('PROJECT_VIEW', { feature: 'projects' }));
  readonly canCreateProjects = computed(() =>
    this.has('PROJECT_CREATE', { feature: 'projects.create' }),
  );
  readonly canUpdateProjects = computed(() =>
    this.has('PROJECT_UPDATE', { feature: 'projects.update' }),
  );

  readonly canUsePromptBuilder = computed(() =>
    canUsePromptBuilder(this.promptPermissionContext()),
  );
  readonly canEnhancePrompt = computed(() => canEnhancePrompt(this.promptPermissionContext()));
  readonly canManageTemplates = computed(() => canManageTemplates(this.promptPermissionContext()));
  readonly canViewPromptTemplates = computed(() =>
    canViewPromptTemplates(this.promptPermissionContext()),
  );
  readonly canViewPromptHistory = computed(() =>
    canViewPromptHistory(this.promptPermissionContext()),
  );
  readonly canViewAiMonitoring = computed(() =>
    this.hasAiAccess('AI_MONITORING_VIEW', 'aiMonitoring'),
  );
  readonly canViewProviderMetrics = computed(() =>
    this.hasAiAccess('AI_PROVIDER_METRICS_VIEW', 'aiMonitoring.providers'),
  );
  readonly canViewLayerAnalytics = computed(() =>
    this.hasAiAccess('AI_LAYER_ANALYTICS_VIEW', 'aiMonitoring.layers'),
  );
  readonly canViewWorkspaceAiUsage = computed(() =>
    this.hasAiAccess('AI_WORKSPACE_USAGE_VIEW', 'aiMonitoring.workspaces'),
  );
  readonly canViewQualityScores = computed(() =>
    this.hasAiAccess('AI_QUALITY_SCORE_VIEW', 'aiMonitoring.quality'),
  );
  readonly canViewAiFailures = computed(() =>
    this.hasAiAccess('AI_FAILURE_VIEW', 'aiMonitoring.failures'),
  );
  readonly canViewUsageBilling = computed(() =>
    this.hasUsageBillingAccess('USAGE_BILLING_VIEW', 'usageBilling'),
  );
  readonly canViewCreditLedger = computed(() =>
    this.hasUsageBillingAccess('CREDIT_LEDGER_VIEW', 'usageBilling.creditLedger'),
  );
  readonly canViewDownloadUsage = computed(() =>
    this.hasUsageBillingAccess('DOWNLOAD_USAGE_VIEW', 'usageBilling.downloads'),
  );
  readonly canViewShareUsage = computed(() =>
    this.hasUsageBillingAccess('SHARE_USAGE_VIEW', 'usageBilling.shares'),
  );
  readonly canViewMasterUsage = computed(() =>
    this.role() === 'MASTER' && this.workspace.isFeatureEnabled('usageBilling.master'),
  );
  readonly canViewPlanUtilization = computed(() =>
    this.role() === 'MASTER' && this.workspace.isFeatureEnabled('usageBilling.planUtilization'),
  );
  readonly canManagePaymentProviders = computed(
    () => this.role() === 'MASTER' && this.workspace.isFeatureEnabled('payments.providers'),
  );
  readonly canManageCreditPackages = computed(
    () => this.role() === 'MASTER' && this.workspace.isFeatureEnabled('payments.creditPackages'),
  );
  readonly canPurchaseSubscription = computed(() =>
    this.hasPaymentAccess('SUBSCRIPTION_PURCHASE', 'payments.subscriptions'),
  );
  readonly canPurchaseCredits = computed(() =>
    this.hasPaymentAccess('CREDIT_PURCHASE', 'payments.credits'),
  );
  readonly canViewPayments = computed(() =>
    this.hasPaymentAccess('PAYMENT_VIEW', 'payments.transactions'),
  );
  readonly canViewInvoices = computed(() =>
    this.hasPaymentAccess('INVOICE_VIEW', 'payments.invoices'),
  );
  readonly canViewNotifications = computed(() =>
    this.hasDay10WorkspaceAccess('NOTIFICATION_VIEW', 'notifications'),
  );
  readonly canManageNotificationPreferences = computed(() =>
    this.hasDay10WorkspaceAccess('NOTIFICATION_PREFERENCE_MANAGE', 'notifications.preferences'),
  );
  readonly canViewActivityFeed = computed(() =>
    this.hasDay10WorkspaceAccess('ACTIVITY_FEED_VIEW', 'activityFeed'),
  );
  readonly canViewAuditLogs = computed(() =>
    this.role() === 'MASTER'
      ? this.workspace.isFeatureEnabled('auditLogs')
      : this.has('AUDIT_LOG_VIEW', { feature: 'auditLogs', requireActiveSubscription: true }),
  );
  readonly canViewMasterMonitoring = computed(() =>
    this.role() === 'MASTER' && this.workspace.isFeatureEnabled('monitoring.master'),
  );
  readonly canViewSystemHealth = computed(() =>
    this.role() === 'MASTER' && this.workspace.isFeatureEnabled('monitoring.systemHealth'),
  );
  readonly canViewMonitoringAlerts = computed(() =>
    this.role() === 'MASTER' && this.workspace.isFeatureEnabled('monitoring.alerts'),
  );
  readonly canViewOwnProfile = computed(() => this.auth.isAuthenticated());
  readonly canEditOwnProfile = computed(() => this.auth.isAuthenticated());
  readonly canChangeOwnPassword = computed(() => this.auth.isAuthenticated());
  readonly canManageProfileImage = computed(() => this.auth.isAuthenticated());
  readonly canViewSecurityActivity = computed(() => this.auth.isAuthenticated());
  readonly canViewUserProfileSupport = computed(() =>
    this.role() === 'MASTER' && this.auth.hasPermission('USER_VIEW'),
  );

  has(permission: Permission, options?: PermissionCheckOptions): boolean {
    if (!this.auth.hasPermission(permission)) {
      return false;
    }

    if (options?.requireActiveSubscription && !this.hasActiveSubscription()) {
      return false;
    }

    return options?.feature ? this.workspace.isFeatureEnabled(options.feature) : true;
  }

  canUseFeature(featureKey: string, options?: Omit<PermissionCheckOptions, 'feature'>): boolean {
    if (options?.requireActiveSubscription && !this.hasActiveSubscription()) {
      return false;
    }

    return this.workspace.isFeatureEnabled(featureKey);
  }

  featureDisabledMessage(featureKey: string): string | null {
    return this.workspace.featureMessage(featureKey);
  }

  private hasAiAccess(permission: Permission, feature: string): boolean {
    if (this.role() === 'MASTER') {
      return this.workspace.isFeatureEnabled(feature);
    }

    return this.has(permission, { feature, requireActiveSubscription: true });
  }

  private hasUsageBillingAccess(permission: Permission, feature: string): boolean {
    if (this.role() === 'MASTER') {
      return this.workspace.isFeatureEnabled(feature);
    }

    return this.has(permission, { feature, requireActiveSubscription: true });
  }

  private hasPaymentAccess(permission: Permission, feature: string): boolean {
    if (this.role() === 'MASTER') {
      return this.workspace.isFeatureEnabled(feature);
    }

    return this.has(permission, { feature, requireActiveSubscription: true });
  }

  private hasDay10WorkspaceAccess(permission: Permission, feature: string): boolean {
    if (this.role() === 'MASTER') {
      return this.workspace.isFeatureEnabled(feature);
    }

    return this.has(permission, { feature, requireActiveSubscription: true });
  }
}
