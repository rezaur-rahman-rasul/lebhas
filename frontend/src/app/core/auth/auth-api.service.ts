import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  AuthSessionResponse,
  LoginRequest,
  LoginResponse,
  LogoutRequest,
  MobileOtpSessionResponse,
  MobileOtpStartRequest,
  MobileOtpStartResponse,
  MobileOtpVerifyRequest,
  RefreshTokenRequest,
  RefreshTokenResponse,
  RegistrationBrandRequest,
  RegistrationEmailStartRequest,
  RegistrationEmailVerifyRequest,
  RegistrationPasswordRequest,
  RegistrationProductServiceRequest,
  RegistrationProjectCampaignRequest,
  RegistrationSessionRequest,
  RegistrationStepResponse,
  RegisterRequest,
  RegisterResponse,
} from '@app/features/auth/models/auth.models';
import { CurrentUser, CurrentUserResponse } from '@app/features/auth/models/user.models';
import { AuthSession } from './auth.types';
import {
  SKIP_AUTH,
  SKIP_ERROR_TOAST,
  SKIP_GLOBAL_LOADING,
  SKIP_REFRESH,
} from './auth-request-context';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly api = inject(ApiService);

  async login(payload: LoginRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<LoginResponse, LoginRequest>(ApiEndpoints.auth.login, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return this.mapSession(unwrapApiResponse(response));
  }

  async register(payload: RegisterRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<RegisterResponse, RegisterRequest>(ApiEndpoints.auth.register, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return this.mapSession(unwrapApiResponse(response));
  }

  async startMobileOtp(payload: MobileOtpStartRequest): Promise<MobileOtpStartResponse> {
    const response = await firstValueFrom(
      this.api.post<MobileOtpStartResponse, MobileOtpStartRequest>(ApiEndpoints.auth.mobileStart, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async verifyMobileOtp(payload: MobileOtpVerifyRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<MobileOtpSessionResponse, MobileOtpVerifyRequest>(ApiEndpoints.auth.mobileVerify, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return this.mapSession(unwrapApiResponse(response));
  }

  async startRegistrationMobile(payload: MobileOtpStartRequest): Promise<RegistrationStepResponse> {
    const response = await firstValueFrom(
      this.api.post<RegistrationStepResponse, MobileOtpStartRequest>(ApiEndpoints.auth.registerMobileStart, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async verifyRegistrationMobile(payload: MobileOtpVerifyRequest): Promise<RegistrationStepResponse> {
    const response = await firstValueFrom(
      this.api.post<RegistrationStepResponse, MobileOtpVerifyRequest>(ApiEndpoints.auth.registerMobileVerify, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async skipRegistrationEmail(payload: RegistrationSessionRequest): Promise<RegistrationStepResponse> {
    const response = await firstValueFrom(
      this.api.post<RegistrationStepResponse, RegistrationSessionRequest>(ApiEndpoints.auth.registerEmailSkip, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async startRegistrationEmail(payload: RegistrationEmailStartRequest): Promise<RegistrationStepResponse> {
    const response = await firstValueFrom(
      this.api.post<RegistrationStepResponse, RegistrationEmailStartRequest>(ApiEndpoints.auth.registerEmailStart, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async verifyRegistrationEmail(payload: RegistrationEmailVerifyRequest): Promise<RegistrationStepResponse> {
    const response = await firstValueFrom(
      this.api.post<RegistrationStepResponse, RegistrationEmailVerifyRequest>(ApiEndpoints.auth.registerEmailVerify, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async setRegistrationPassword(payload: RegistrationPasswordRequest): Promise<RegistrationStepResponse> {
    const response = await firstValueFrom(
      this.api.post<RegistrationStepResponse, RegistrationPasswordRequest>(ApiEndpoints.auth.registerPassword, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async completeRegistrationBrand(payload: RegistrationBrandRequest): Promise<RegistrationStepResponse> {
    const response = await firstValueFrom(
      this.api.post<RegistrationStepResponse, RegistrationBrandRequest>(ApiEndpoints.auth.registerBrand, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async completeRegistrationProductService(payload: RegistrationProductServiceRequest): Promise<RegistrationStepResponse> {
    const response = await firstValueFrom(
      this.api.post<RegistrationStepResponse, RegistrationProductServiceRequest>(ApiEndpoints.auth.registerProductService, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return unwrapApiResponse(response);
  }

  async completeRegistrationProjectCampaign(payload: RegistrationProjectCampaignRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<AuthSessionResponse, RegistrationProjectCampaignRequest>(ApiEndpoints.auth.registerProjectCampaign, payload, {
        context: this.authRequestContext({ skipAuth: true, skipRefresh: true }),
      }),
    );

    return this.mapSession(unwrapApiResponse(response));
  }

  async refreshSession(payload: RefreshTokenRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<RefreshTokenResponse, RefreshTokenRequest>(
        ApiEndpoints.auth.refresh,
        payload,
        {
          context: this.authRequestContext({
            skipAuth: true,
            skipRefresh: true,
            skipGlobalLoading: true,
          }),
        },
      ),
    );

    return this.mapSession(unwrapApiResponse(response));
  }

  async getCurrentUser(): Promise<CurrentUser> {
    const response = await firstValueFrom(
      this.api.get<CurrentUserResponse>(ApiEndpoints.auth.me, {
        context: this.authRequestContext({
          skipGlobalLoading: true,
          skipRefresh: true,
        }),
      }),
    );

    return this.mapUser(unwrapApiResponse(response));
  }

  async logout(payload: LogoutRequest): Promise<void> {
    await firstValueFrom(
      this.api.post<void, LogoutRequest>(ApiEndpoints.auth.logout, payload, {
        context: this.authRequestContext({
          skipErrorToast: true,
          skipRefresh: true,
        }),
      }),
    );
  }

  private mapSession(response: AuthSessionResponse): AuthSession {
    return {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      accessTokenExpiresAt: response.accessTokenExpiresAt,
      refreshTokenExpiresAt: response.refreshTokenExpiresAt,
      user: this.mapUser(response.user),
      workspaceId: response.workspaceId,
      brandId: response.brandId,
      productServiceId: response.productServiceId,
      projectCampaignId: response.projectCampaignId,
      nextStep: response.nextStep,
    };
  }

  private mapUser(response: CurrentUserResponse): CurrentUser {
    const fullName = `${response.firstName} ${response.lastName}`.trim();

    return {
      id: response.id,
      firstName: response.firstName,
      lastName: response.lastName,
      name: fullName,
      fullName,
      email: response.email,
      phone: response.phone,
      role: response.role,
      status: response.status,
      emailVerified: response.emailVerified,
      lastLoginAt: response.lastLoginAt,
      createdAt: response.createdAt,
      updatedAt: response.updatedAt,
      permissions: response.permissions,
      workspaceId: response.workspaceId,
      workspaceName: null,
      workspace: {
        id: response.workspaceId,
        name: null,
      },
    };
  }

  private authRequestContext(options?: {
    readonly skipAuth?: boolean;
    readonly skipRefresh?: boolean;
    readonly skipErrorToast?: boolean;
    readonly skipGlobalLoading?: boolean;
  }): HttpContext {
    return new HttpContext()
      .set(SKIP_AUTH, options?.skipAuth ?? false)
      .set(SKIP_REFRESH, options?.skipRefresh ?? false)
      .set(SKIP_ERROR_TOAST, options?.skipErrorToast ?? true)
      .set(SKIP_GLOBAL_LOADING, options?.skipGlobalLoading ?? false);
  }
}
