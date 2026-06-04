const API_PREFIX = '/api/v1';

const path = (value: string): string => encodeURIComponent(value);

export const ApiEndpoints = {
  auth: {
    register: `${API_PREFIX}/auth/register`,
    login: `${API_PREFIX}/auth/login`,
    refresh: `${API_PREFIX}/auth/refresh`,
    logout: `${API_PREFIX}/auth/logout`,
    logoutAll: `${API_PREFIX}/auth/logout-all`,
    me: `${API_PREFIX}/auth/me`,
    forgotPassword: `${API_PREFIX}/auth/forgot-password`,
    resetPassword: `${API_PREFIX}/auth/reset-password`,
    verifyEmail: `${API_PREFIX}/auth/verify-email`,
    resendVerification: `${API_PREFIX}/auth/resend-verification`,
  },
  profile: {
    me: `${API_PREFIX}/profile/me`,
    accountSettings: `${API_PREFIX}/profile/me/account-settings`,
    changePassword: `${API_PREFIX}/profile/me/change-password`,
    profileImageUploadUrl: `${API_PREFIX}/profile/me/profile-image/upload-url`,
    profileImageConfirm: `${API_PREFIX}/profile/me/profile-image/confirm`,
    profileImage: `${API_PREFIX}/profile/me/profile-image`,
    securityActivity: `${API_PREFIX}/profile/me/security-activity`,
    sessions: `${API_PREFIX}/profile/me/sessions`,
    session: (sessionId: string) => `${API_PREFIX}/profile/me/sessions/${path(sessionId)}`,
    otherSessions: `${API_PREFIX}/profile/me/sessions/others`,
  },
  workspaces: {
    my: `${API_PREFIX}/workspaces/my`,
    create: `${API_PREFIX}/workspaces`,
    detail: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}`,
    context: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/context`,
    members: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/members`,
    memberInvite: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/members/invite`,
    member: (workspaceId: string, memberId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/members/${path(memberId)}`,
    settings: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/settings`,
  },
  brands: {
    list: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/brands`,
    detail: (workspaceId: string, brandId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/brands/${path(brandId)}`,
    summary: (workspaceId: string, brandId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/brands/${path(brandId)}/summary`,
    productServices: (workspaceId: string, brandId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/brands/${path(brandId)}/product-services`,
  },
  productServices: {
    list: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/product-services`,
    detail: (workspaceId: string, productServiceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/product-services/${path(productServiceId)}`,
    projects: (workspaceId: string, productServiceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/product-services/${path(productServiceId)}/projects`,
  },
  projects: {
    list: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/projects`,
    detail: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}`,
  },
  assets: {
    upload: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/assets/upload`,
    uploadUrl: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/assets/upload-url`,
    confirm: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/assets/confirm-upload`,
    list: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/assets`,
    detail: (workspaceId: string, assetId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/assets/${path(assetId)}`,
    previewUrl: (workspaceId: string, assetId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/assets/${path(assetId)}/preview-url`,
    downloadUrl: (workspaceId: string, assetId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/assets/${path(assetId)}/download-url`,
    projectList: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/assets`,
    projectUploadUrl: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/assets/upload-url`,
    projectUpload: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/assets/upload`,
    folders: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/asset-folders`,
    folder: (workspaceId: string, folderId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/asset-folders/${path(folderId)}`,
  },
  prompts: {
    context: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/prompts/context`,
    validate: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/prompts/validate`,
    drafts: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/prompts/drafts`,
    draft: (workspaceId: string, projectId: string, draftId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/prompts/drafts/${path(draftId)}`,
    enhance: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/prompts/enhance`,
    suggestions: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/prompts/suggestions`,
    templates: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/prompts/templates`,
    reuseTemplate: (workspaceId: string, projectId: string, templateId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/prompts/templates/${path(templateId)}/reuse`,
    history: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/prompts/history`,
  },
  creativeRequests: {
    listByProject: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/creative-requests`,
    fromPrompt: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/creative-requests/from-prompt`,
    detail: (workspaceId: string, creativeRequestId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/creative-requests/${path(creativeRequestId)}`,
    validate: (workspaceId: string, creativeRequestId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/creative-requests/${path(creativeRequestId)}/validate`,
    generationPreview: (workspaceId: string, creativeRequestId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/creative-requests/${path(creativeRequestId)}/generation/preview`,
    generationQueue: (workspaceId: string, creativeRequestId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/creative-requests/${path(creativeRequestId)}/generation/queue`,
    generatedVersions: (workspaceId: string, creativeRequestId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/creative-requests/${path(creativeRequestId)}/generated-versions`,
  },
  generationJobs: {
    detail: (workspaceId: string, generationJobId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generation-jobs/${path(generationJobId)}`,
    fullDetail: (workspaceId: string, generationJobId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generation-jobs/${path(generationJobId)}/detail`,
    retry: (workspaceId: string, generationJobId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generation-jobs/${path(generationJobId)}/retry`,
  },
  generatedVersions: {
    approvals: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/approvals/generated-versions`,
    detail: (workspaceId: string, generatedVersionId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generated-versions/${path(generatedVersionId)}`,
    approve: (workspaceId: string, generatedVersionId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generated-versions/${path(generatedVersionId)}/approve`,
    reject: (workspaceId: string, generatedVersionId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generated-versions/${path(generatedVersionId)}/reject`,
    requestChanges: (workspaceId: string, generatedVersionId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generated-versions/${path(generatedVersionId)}/request-changes`,
    approvalHistory: (workspaceId: string, generatedVersionId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generated-versions/${path(generatedVersionId)}/approval-history`,
    downloadUrl: (workspaceId: string, generatedVersionId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generated-versions/${path(generatedVersionId)}/download-url`,
    shareLinks: (workspaceId: string, generatedVersionId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generated-versions/${path(generatedVersionId)}/share-links`,
    shareLinkRevoke: (workspaceId: string, generatedVersionId: string, shareLinkId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/generated-versions/${path(generatedVersionId)}/share-links/${path(shareLinkId)}/revoke`,
  },
  publicShare: {
    detail: (token: string) => `${API_PREFIX}/public/share/${path(token)}`,
    downloadUrl: (token: string) => `${API_PREFIX}/public/share/${path(token)}/download-url`,
  },
  textTools: {
    run: (workspaceId: string, projectId: string, tool: 'post' | 'caption' | 'ads-copy' | 'hashtags') =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/text-tools/${tool}`,
    history: (workspaceId: string, projectId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/projects/${path(projectId)}/text-tools/history`,
    detail: (workspaceId: string, textToolOutputId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/text-tools/${path(textToolOutputId)}`,
  },
  notifications: {
    list: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/notifications`,
    unreadCount: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/notifications/unread-count`,
    read: (workspaceId: string, notificationId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/notifications/${path(notificationId)}/read`,
    readAll: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/notifications/read-all`,
    preferences: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/notification-preferences`,
  },
  usage: {
    summary: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/usage-summary`,
    currentMonth: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/usage-summary/current-month`,
    credits: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/credits`,
    creditLedger: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/credit-ledger`,
    billingLogs: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/usage-billing-logs`,
    downloadUsage: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/download-usage`,
    shareUsage: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/share-usage`,
    monthlySnapshots: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/monthly-usage-snapshots`,
    masterWorkspaces: `${API_PREFIX}/master/usage/workspaces`,
    masterWorkspace: (workspaceId: string) =>
      `${API_PREFIX}/master/usage/workspaces/${path(workspaceId)}`,
    masterAiCosts: `${API_PREFIX}/master/usage/ai-costs`,
    masterTopCostWorkspaces: `${API_PREFIX}/master/usage/top-cost-workspaces`,
    masterPlanUtilization: `${API_PREFIX}/master/usage/plan-utilization`,
  },
  activity: {
    feed: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/activity-feed`,
    timeline: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/timeline`,
    auditLogs: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/audit-logs`,
  },
  billing: {
    subscription: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/subscription`,
    purchaseSubscription: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/subscriptions/purchase`,
    upgradeSubscription: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/subscriptions/upgrade`,
    renewSubscription: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/subscriptions/renew`,
    purchaseCredits: (workspaceId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/credits/purchase`,
    payments: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/payments`,
    payment: (workspaceId: string, paymentTransactionId: string) =>
      `${API_PREFIX}/workspaces/${path(workspaceId)}/payments/${path(paymentTransactionId)}`,
    invoices: (workspaceId: string) => `${API_PREFIX}/workspaces/${path(workspaceId)}/invoices`,
  },
  master: {
    dashboardSummary: `${API_PREFIX}/master/dashboard/summary`,
    goLiveReadiness: `${API_PREFIX}/master/go-live-readiness`,
    workspaces: `${API_PREFIX}/master/workspaces`,
    workspace: (workspaceId: string) => `${API_PREFIX}/master/workspaces/${path(workspaceId)}`,
    users: `${API_PREFIX}/master/users`,
    user: (userId: string) => `${API_PREFIX}/master/users/${path(userId)}`,
    userStatus: (userId: string) => `${API_PREFIX}/master/users/${path(userId)}/status`,
    pricingPlans: `${API_PREFIX}/master/pricing-plans`,
    pricingPlan: (pricingPlanId: string) => `${API_PREFIX}/master/pricing-plans/${path(pricingPlanId)}`,
    pricingPlanActivate: (pricingPlanId: string) =>
      `${API_PREFIX}/master/pricing-plans/${path(pricingPlanId)}/activate`,
    pricingPlanDeactivate: (pricingPlanId: string) =>
      `${API_PREFIX}/master/pricing-plans/${path(pricingPlanId)}/deactivate`,
    pricingPlanFeaturePolicy: (pricingPlanId: string) =>
      `${API_PREFIX}/master/pricing-plans/${path(pricingPlanId)}/feature-policy`,
    paymentProviders: `${API_PREFIX}/master/payment-providers`,
    paymentProvider: (providerId: string) => `${API_PREFIX}/master/payment-providers/${path(providerId)}`,
    paymentProviderConfigurations: `${API_PREFIX}/master/payment-provider-configurations`,
    paymentProviderConfiguration: (configurationId: string) =>
      `${API_PREFIX}/master/payment-provider-configurations/${path(configurationId)}`,
    creditPackages: `${API_PREFIX}/master/credit-packages`,
    creditPackage: (creditPackageId: string) =>
      `${API_PREFIX}/master/credit-packages/${path(creditPackageId)}`,
    auditLogs: `${API_PREFIX}/master/audit-logs`,
    aiProviderMetrics: `${API_PREFIX}/master/ai/providers/metrics`,
    aiProviderHealth: `${API_PREFIX}/master/ai/provider-health`,
    providers: `${API_PREFIX}/master/providers`,
    provider: (providerId: string) => `${API_PREFIX}/master/providers/${path(providerId)}`,
    providerCredentials: (providerId: string) =>
      `${API_PREFIX}/master/providers/${path(providerId)}/credentials`,
    providerTestConnection: (providerId: string) =>
      `${API_PREFIX}/master/providers/${path(providerId)}/test-connection`,
    providerStatus: (providerId: string) =>
      `${API_PREFIX}/master/providers/${path(providerId)}/status`,
    aiLayerAnalytics: `${API_PREFIX}/master/ai/layer-analytics`,
    aiCostUsage: `${API_PREFIX}/master/ai/cost-usage`,
    aiWorkspaceUsage: (workspaceId: string) =>
      `${API_PREFIX}/master/ai/workspaces/${path(workspaceId)}/usage`,
    aiFailures: `${API_PREFIX}/master/ai/failures`,
  },
  health: {
    live: '/health/live',
    ready: '/health/ready',
    dependencies: '/health/dependencies',
  },
} as const;
