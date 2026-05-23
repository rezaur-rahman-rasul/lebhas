import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiResponse } from '@app/shared/models/api-response.model';
import { environment } from '@env/environment';
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
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly authBasePath = environment.authApiPrefix;

  async login(payload: LoginRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<LoginResponse, LoginRequest>(`${this.authBasePath}/login`, payload, {
        context: this.authRequestContext({ skipRefresh: true }),
      }),
    );

    return this.mapSession(this.unwrap(response));
  }

  async register(payload: RegisterRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<RegisterResponse, RegisterRequest>(`${this.authBasePath}/register`, payload, {
        context: this.authRequestContext({ skipRefresh: true }),
      }),
    );

    return this.mapSession(this.unwrap(response));
  }

  async refreshSession(payload: RefreshTokenRequest): Promise<AuthSession> {
    const response = await firstValueFrom(
      this.api.post<RefreshTokenResponse, RefreshTokenRequest>(
        `${this.authBasePath}/refresh`,
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

    return this.mapSession(this.unwrap(response));
  }

  async getCurrentUser(): Promise<CurrentUser> {
    const response = await firstValueFrom(
      this.api.get<CurrentUserResponse>(`${this.authBasePath}/me`, {
        context: this.authRequestContext({
          skipGlobalLoading: true,
          skipRefresh: true,
        }),
      }),
    );

    return this.mapUser(this.unwrap(response));
  }

  async logout(payload: LogoutRequest): Promise<void> {
    await firstValueFrom(
      this.api.post<void, LogoutRequest>(`${this.authBasePath}/logout`, payload, {
        context: this.authRequestContext({
          skipErrorToast: true,
          skipRefresh: true,
        }),
      }),
    );
  }

  private unwrap<T>(response: ApiResponse<T>): T {
    return response.data;
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
