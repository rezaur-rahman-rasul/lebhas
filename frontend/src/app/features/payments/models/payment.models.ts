export enum PaymentProviderType {
  Sslcommerz = 'SSLCOMMERZ',
  Bkash = 'BKASH',
  Nagad = 'NAGAD',
  Stripe = 'STRIPE',
  Manual = 'MANUAL',
}

export enum PaymentPurpose {
  SubscriptionPurchase = 'SUBSCRIPTION_PURCHASE',
  PlanUpgrade = 'PLAN_UPGRADE',
  PlanRenewal = 'PLAN_RENEWAL',
  CreditPurchase = 'CREDIT_PURCHASE',
}

export enum PaymentStatus {
  Initiated = 'INITIATED',
  Pending = 'PENDING',
  Success = 'SUCCESS',
  Failed = 'FAILED',
  Cancelled = 'CANCELLED',
  Expired = 'EXPIRED',
  Refunded = 'REFUNDED',
}

export enum BillingCycle {
  Monthly = 'MONTHLY',
  Yearly = 'YEARLY',
}

export enum InvoiceType {
  Subscription = 'SUBSCRIPTION',
  CreditPurchase = 'CREDIT_PURCHASE',
}

export enum EnvironmentType {
  Sandbox = 'SANDBOX',
  Live = 'LIVE',
}

export interface PaymentProvider {
  readonly id: string;
  readonly name: string;
  readonly code: string;
  readonly providerType: PaymentProviderType | string;
  readonly isEnabled: boolean;
  readonly sandboxEnabled: boolean;
  readonly liveEnabled: boolean;
  readonly priority: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PaymentProviderConfiguration {
  readonly id: string;
  readonly providerId: string;
  readonly providerName: string;
  readonly environmentType: EnvironmentType | string;
  readonly apiBaseUrl: string | null;
  readonly merchantId: string | null;
  readonly successUrl: string | null;
  readonly failureUrl: string | null;
  readonly cancelUrl: string | null;
  readonly isActive: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PaymentTransaction {
  readonly id: string;
  readonly workspaceId: string;
  readonly userId: string;
  readonly userDisplayName?: string | null;
  readonly userProfileImageUrl?: string | null;
  readonly initiatedByName?: string | null;
  readonly requestedByName?: string | null;
  readonly providerId: string | null;
  readonly providerName: string | null;
  readonly paymentPurpose: PaymentPurpose | string;
  readonly referenceType: string | null;
  readonly referenceId: string | null;
  readonly amount: number;
  readonly currency: string;
  readonly providerTransactionId: string | null;
  readonly providerSessionId: string | null;
  readonly status: PaymentStatus | string;
  readonly failureReason: string | null;
  readonly initiatedAt: string | null;
  readonly completedAt: string | null;
  readonly cancelledAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreditPackage {
  readonly id: string;
  readonly name: string;
  readonly code: string;
  readonly credits: number;
  readonly bonusCredits: number;
  readonly price: number;
  readonly currency: string;
  readonly isActive: boolean;
  readonly sortOrder: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreditPurchaseOrder {
  readonly id: string;
  readonly workspaceId: string;
  readonly creditPackageId: string;
  readonly requestedBy: string;
  readonly requestedByName?: string | null;
  readonly requestedByProfileImageUrl?: string | null;
  readonly credits: number;
  readonly amount: number;
  readonly currency: string;
  readonly status: PaymentStatus | string;
  readonly paymentTransactionId: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface SubscriptionOrder {
  readonly id: string;
  readonly workspaceId: string;
  readonly pricingPlanId: string;
  readonly requestedBy: string;
  readonly requestedByName?: string | null;
  readonly requestedByProfileImageUrl?: string | null;
  readonly billingCycle: BillingCycle | string;
  readonly amount: number;
  readonly currency: string;
  readonly status: PaymentStatus | string;
  readonly paymentTransactionId: string | null;
  readonly startsAt: string | null;
  readonly expiresAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface Invoice {
  readonly id: string;
  readonly workspaceId: string;
  readonly userDisplayName?: string | null;
  readonly userProfileImageUrl?: string | null;
  readonly paymentTransactionId: string | null;
  readonly invoiceNumber: string;
  readonly invoiceType: InvoiceType | string;
  readonly amount: number;
  readonly currency: string;
  readonly status: PaymentStatus | string;
  readonly issuedAt: string | null;
  readonly paidAt: string | null;
  readonly createdAt: string;
}

export interface PaymentSessionResponse {
  readonly paymentTransactionId: string;
  readonly paymentStatus: PaymentStatus | string;
  readonly providerName: string | null;
  readonly providerSessionId: string | null;
  readonly paymentRedirectUrl: string | null;
  readonly expiresAt: string | null;
}

export interface PricingPlanForPurchase {
  readonly id: string;
  readonly name: string;
  readonly code: string;
  readonly billingCycle: BillingCycle | string;
  readonly price: number;
  readonly currency: string;
  readonly description?: string | null;
  readonly features?: Readonly<Record<string, unknown>> | null;
  readonly limits?: Readonly<Record<string, unknown>> | null;
  readonly isActive?: boolean;
  readonly sortOrder?: number | null;
}

export interface PricingPlan {
  readonly id: string;
  readonly name: string;
  readonly code: string;
  readonly description: string | null;
  readonly monthlyPrice: number;
  readonly yearlyPrice: number;
  readonly currency: string;
  readonly defaultPlan: boolean;
  readonly active: boolean;
  readonly sortOrder: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PlanFeaturePolicy {
  readonly id: string;
  readonly pricingPlanId: string;
  readonly maxGeneratedVersionsPerRequest: number | null;
  readonly maxBrands: number | null;
  readonly maxProductServices: number | null;
  readonly maxProjects: number | null;
  readonly maxTeamMembers: number | null;
  readonly maxStorageGb: number | null;
  readonly monthlyCreditLimit: number | null;
  readonly allowApprovalWorkflow: boolean;
  readonly allowPublicShareLinks: boolean;
  readonly allowVideoGeneration: boolean;
  readonly allowAdvancedPromptIntelligence: boolean;
  readonly allowTeamCollaboration: boolean;
  readonly allowExportWithoutWatermark: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PricingPlanDetail {
  readonly pricingPlan: PricingPlan;
  readonly featurePolicy: PlanFeaturePolicy | null;
}

export interface CurrentSubscription {
  readonly id: string;
  readonly workspaceId: string;
  readonly pricingPlanId: string;
  readonly pricingPlanName: string;
  readonly billingCycle: BillingCycle | string;
  readonly status: string;
  readonly startsAt: string | null;
  readonly expiresAt: string | null;
  readonly renewsAt?: string | null;
}

export interface PaymentFilters {
  readonly status?: PaymentStatus | string | null;
  readonly paymentPurpose?: PaymentPurpose | string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly page?: number | null;
  readonly size?: number | null;
}

export interface InvoiceFilters {
  readonly invoiceType?: InvoiceType | string | null;
  readonly status?: PaymentStatus | string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly page?: number | null;
  readonly size?: number | null;
}

export interface PaymentProviderPayload {
  readonly name: string;
  readonly code: string;
  readonly providerType: PaymentProviderType | string;
  readonly isEnabled: boolean;
  readonly sandboxEnabled: boolean;
  readonly liveEnabled: boolean;
  readonly priority: number;
}

export interface PricingPlanPayload {
  readonly name: string;
  readonly code: string;
  readonly description?: string | null;
  readonly monthlyPrice: number;
  readonly yearlyPrice: number;
  readonly currency: string;
  readonly defaultPlan: boolean;
  readonly active: boolean;
  readonly sortOrder: number;
}

export interface PlanFeaturePolicyPayload {
  readonly maxGeneratedVersionsPerRequest?: number | null;
  readonly maxBrands?: number | null;
  readonly maxProductServices?: number | null;
  readonly maxProjects?: number | null;
  readonly maxTeamMembers?: number | null;
  readonly maxStorageGb?: number | null;
  readonly monthlyCreditLimit?: number | null;
  readonly allowApprovalWorkflow: boolean;
  readonly allowPublicShareLinks: boolean;
  readonly allowVideoGeneration: boolean;
  readonly allowAdvancedPromptIntelligence: boolean;
  readonly allowTeamCollaboration: boolean;
  readonly allowExportWithoutWatermark: boolean;
}

export interface PaymentProviderConfigurationPayload {
  readonly providerId: string;
  readonly environmentType: EnvironmentType | string;
  readonly apiBaseUrl?: string | null;
  readonly merchantId?: string | null;
  readonly successUrl?: string | null;
  readonly failureUrl?: string | null;
  readonly cancelUrl?: string | null;
  readonly isActive: boolean;
  readonly secrets?: Readonly<Record<string, string | null | undefined>>;
}

export interface CreditPackagePayload {
  readonly name: string;
  readonly code: string;
  readonly credits: number;
  readonly bonusCredits: number;
  readonly price: number;
  readonly currency: string;
  readonly isActive: boolean;
  readonly sortOrder: number;
}

export interface SubscriptionPurchasePayload {
  readonly pricingPlanId: string;
  readonly billingCycle: BillingCycle | string;
  readonly returnUrl?: string | null;
}

export type SubscriptionUpgradePayload = SubscriptionPurchasePayload;
export type SubscriptionRenewPayload = SubscriptionPurchasePayload;

export interface CreditPurchasePayload {
  readonly creditPackageId: string;
  readonly returnUrl?: string | null;
}

export interface SelectedSubscriptionSummary {
  readonly pricingPlanId: string;
  readonly packageName: string;
  readonly billingCycle: BillingCycle | string;
  readonly amount: number;
  readonly currency: string;
}

export interface SelectedCreditPurchaseSummary {
  readonly creditPackageId: string;
  readonly packageName: string;
  readonly credits: number;
  readonly bonusCredits: number;
  readonly amount: number;
  readonly currency: string;
}

export interface PaymentActionResult {
  readonly ok: boolean;
  readonly message?: string;
}
