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
  RefreshTokenRequest,
  RefreshTokenResponse,
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
        context: this.authRequestContext({ skipRefresh: true }),
      }),
    );

    return this.mapSession(unwrapApiResponse(response));
  }

  async register(payload: RegisterRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<RegisterResponse, RegisterRequest>(ApiEndpoints.auth.register, payload, {
        context: this.authRequestContext({ skipRefresh: true }),
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
