import { ApiError } from '@app/shared/models/api-response.model';
import { CurrentUser } from '@app/features/auth/models/user.models';

export interface AuthTokens {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly accessTokenExpiresAt: string;
  readonly refreshTokenExpiresAt: string;
}

export interface AuthSession extends AuthTokens {
  readonly user: CurrentUser;
}

export interface PersistedAuthSession extends AuthTokens {
  readonly activeWorkspaceId: string | null;
}

export interface StoredAuthSession extends AuthSession, PersistedAuthSession {}

export interface AuthActionFailure {
  readonly ok: false;
  readonly status: number;
  readonly message: string;
  readonly errors: readonly ApiError[];
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export interface AuthActionSuccess<T = void> {
  readonly ok: true;
  readonly data: T;
}

export type AuthActionResult<T = void> = AuthActionSuccess<T> | AuthActionFailure;
