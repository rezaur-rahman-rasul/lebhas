import { Injectable } from '@angular/core';

import { PersistedAuthSession } from './auth.types';

const ACCESS_TOKEN_KEY = 'creative_saas.access_token';
const REFRESH_TOKEN_KEY = 'creative_saas.refresh_token';
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'creative_saas.access_token_expires_at';
const REFRESH_TOKEN_EXPIRES_AT_KEY = 'creative_saas.refresh_token_expires_at';
const ACTIVE_WORKSPACE_ID_KEY = 'creative_saas.active_workspace_id';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  getSessionSource(): 'local' | 'session' | null {
    if (this.readSession(globalThis.localStorage)) {
      return 'local';
    }

    if (this.readSession(globalThis.sessionStorage)) {
      return 'session';
    }

    return null;
  }

  getAccessToken(): string | null {
    return this.read(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return this.read(REFRESH_TOKEN_KEY);
  }

  getSession(): PersistedAuthSession | null {
    return this.readSession(globalThis.localStorage) ?? this.readSession(globalThis.sessionStorage);
  }

  setSession(session: PersistedAuthSession, options?: { readonly persistent?: boolean }): void {
    const persistent = options?.persistent ?? true;
    const storage = persistent ? globalThis.localStorage : globalThis.sessionStorage;

    this.clear();
    this.write(storage, ACCESS_TOKEN_KEY, session.accessToken);
    this.write(storage, REFRESH_TOKEN_KEY, session.refreshToken);
    this.write(storage, ACCESS_TOKEN_EXPIRES_AT_KEY, session.accessTokenExpiresAt);
    this.write(storage, REFRESH_TOKEN_EXPIRES_AT_KEY, session.refreshTokenExpiresAt);

    if (session.activeWorkspaceId) {
      this.write(storage, ACTIVE_WORKSPACE_ID_KEY, session.activeWorkspaceId);
    }
  }

  clear(): void {
    this.remove(globalThis.localStorage, ACCESS_TOKEN_KEY);
    this.remove(globalThis.localStorage, REFRESH_TOKEN_KEY);
    this.remove(globalThis.localStorage, ACCESS_TOKEN_EXPIRES_AT_KEY);
    this.remove(globalThis.localStorage, REFRESH_TOKEN_EXPIRES_AT_KEY);
    this.remove(globalThis.localStorage, ACTIVE_WORKSPACE_ID_KEY);
    this.remove(globalThis.sessionStorage, ACCESS_TOKEN_KEY);
    this.remove(globalThis.sessionStorage, REFRESH_TOKEN_KEY);
    this.remove(globalThis.sessionStorage, ACCESS_TOKEN_EXPIRES_AT_KEY);
    this.remove(globalThis.sessionStorage, REFRESH_TOKEN_EXPIRES_AT_KEY);
    this.remove(globalThis.sessionStorage, ACTIVE_WORKSPACE_ID_KEY);
  }

  private read(key: string): string | null {
    return this.readFrom(globalThis.localStorage, key) ?? this.readFrom(globalThis.sessionStorage, key);
  }

  private readSession(storage?: Storage): PersistedAuthSession | null {
    const accessToken = this.readFrom(storage, ACCESS_TOKEN_KEY);
    const refreshToken = this.readFrom(storage, REFRESH_TOKEN_KEY);
    const accessTokenExpiresAt = this.readFrom(storage, ACCESS_TOKEN_EXPIRES_AT_KEY);
    const refreshTokenExpiresAt = this.readFrom(storage, REFRESH_TOKEN_EXPIRES_AT_KEY);

    if (!accessToken || !refreshToken || !accessTokenExpiresAt || !refreshTokenExpiresAt) {
      return null;
    }

    return {
      accessToken,
      refreshToken,
      accessTokenExpiresAt,
      refreshTokenExpiresAt,
      activeWorkspaceId: this.readFrom(storage, ACTIVE_WORKSPACE_ID_KEY),
    };
  }

  private readFrom(storage: Storage | undefined, key: string): string | null {
    try {
      return storage?.getItem(key) ?? null;
    } catch {
      return null;
    }
  }

  private write(storage: Storage | undefined, key: string, value: string): void {
    try {
      storage?.setItem(key, value);
    } catch {
      // Ignore storage availability issues.
    }
  }

  private remove(storage: Storage | undefined, key: string): void {
    try {
      storage?.removeItem(key);
    } catch {
      // Ignore storage availability issues.
    }
  }
}
