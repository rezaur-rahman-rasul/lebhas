import { CurrentUserResponse } from './user.models';

export interface LoginRequest {
  readonly email: string;
  readonly password: string;
  readonly workspaceId?: string | null;
}

export interface RegisterRequest {
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly phone?: string | null;
  readonly password: string;
  readonly confirmPassword: string;
  readonly workspaceId?: string | null;
  readonly invitationToken?: string | null;
}

export interface RefreshTokenRequest {
  readonly refreshToken: string;
}

export interface MobileOtpStartRequest {
  readonly mobileNumber: string;
}

export interface MobileOtpStartResponse {
  readonly otpToken: string;
  readonly mobileNumberMasked: string;
  readonly isNewUser: boolean;
  readonly resendAfterSeconds: number;
  readonly otpLength: number;
}

export interface MobileOtpVerifyRequest {
  readonly otpToken: string;
  readonly otp: string;
  readonly deviceId?: string | null;
}

export interface RegistrationSessionRequest {
  readonly registrationSessionToken: string;
}

export interface RegistrationEmailStartRequest extends RegistrationSessionRequest {
  readonly email: string;
}

export interface RegistrationEmailVerifyRequest extends RegistrationSessionRequest {
  readonly otp: string;
}

export interface RegistrationPasswordRequest extends RegistrationSessionRequest {
  readonly password: string;
  readonly confirmPassword: string;
}

export interface RegistrationBrandRequest extends RegistrationSessionRequest {
  readonly brandName: string;
  readonly deviceId?: string | null;
}

export interface RegistrationProductServiceRequest extends RegistrationSessionRequest {
  readonly productServiceName: string;
}

export interface RegistrationProjectCampaignRequest extends RegistrationSessionRequest {
  readonly projectCampaignName: string;
  readonly deviceId?: string | null;
}

export type RegistrationNextStep =
  | 'MOBILE_OTP'
  | 'EMAIL_OPTIONAL'
  | 'EMAIL_OTP'
  | 'PASSWORD_SETUP'
  | 'BRAND_NAME'
  | 'PRODUCT_SERVICE_NAME'
  | 'PROJECT_CAMPAIGN_NAME'
  | 'CREATIVE_GENERATOR';

export interface RegistrationStepResponse {
  readonly registrationSessionToken: string;
  readonly nextStep: RegistrationNextStep;
  readonly mobileNumberMasked: string | null;
  readonly emailMasked: string | null;
  readonly isNewUser: boolean;
  readonly resendAfterSeconds: number;
  readonly otpLength: number;
  readonly creditsGranted: number;
  readonly workspaceId?: string | null;
  readonly brandId?: string | null;
  readonly productServiceId?: string | null;
  readonly projectCampaignId?: string | null;
}

export interface MobileOtpVerifyResponse extends AuthSessionResponse {
  readonly workspaceId: string;
  readonly isNewUser: boolean;
  readonly freeCreditsGranted: number;
}

export interface LogoutRequest {
  readonly refreshToken: string;
  readonly logoutAllDevices?: boolean;
}

export interface AuthSessionResponse {
  readonly accessToken: string;
  readonly accessTokenExpiresAt: string;
  readonly refreshToken: string;
  readonly refreshTokenExpiresAt: string;
  readonly user: CurrentUserResponse;
  readonly workspaceId?: string | null;
  readonly brandId?: string | null;
  readonly productServiceId?: string | null;
  readonly projectCampaignId?: string | null;
  readonly nextStep?: RegistrationNextStep | null;
}

export interface RememberedProfile {
  readonly id: string;
  readonly name: string;
  readonly avatarUrl: string | null;
  readonly email: string;
  readonly lastUsedAt: string;
}

export type LoginResponse = AuthSessionResponse;
export type RegisterResponse = AuthSessionResponse;
export type RefreshTokenResponse = AuthSessionResponse;
export type MobileOtpSessionResponse = MobileOtpVerifyResponse;
