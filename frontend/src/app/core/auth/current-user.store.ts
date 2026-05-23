import { Injectable, computed, inject, signal } from '@angular/core';

import { CurrentUser, Permission, UserRole } from '@app/features/auth/models/user.models';
import { AuthSession } from './auth.types';
import { TokenStorageService } from './token-storage.service';
import { hasAnyRole, hasPermission } from './permissions';

type AuthResolutionState = 'pending' | 'authenticated' | 'anonymous';

@Injectable({ providedIn: 'root' })
export class CurrentUserStore {
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly restoredSession = this.tokenStorage.getSession();
  private readonly persistentSessionSignal = signal(
    this.tokenStorage.getSessionSource() !== 'session',
  );

  private readonly accessTokenSignal = signal<string | null>(
    this.restoredSession?.accessToken ?? null,
  );
  private readonly refreshTokenSignal = signal<string | null>(
    this.restoredSession?.refreshToken ?? null,
  );
  private readonly accessTokenExpiresAtSignal = signal<string | null>(
    this.restoredSession?.accessTokenExpiresAt ?? null,
  );
  private readonly refreshTokenExpiresAtSignal = signal<string | null>(
    this.restoredSession?.refreshTokenExpiresAt ?? null,
  );
  private readonly currentUserSignal = signal<CurrentUser | null>(null);
  private readonly activeWorkspaceIdSignal = signal<string | null>(
    this.restoredSession?.activeWorkspaceId ?? null,
  );
  private readonly authLoadingSignal = signal(false);
  private readonly authErrorSignal = signal<string | null>(null);
  private readonly authResolutionSignal = signal<AuthResolutionState>(
    this.restoredSession ? 'pending' : 'anonymous',
  );

  readonly accessToken = this.accessTokenSignal.asReadonly();
  readonly refreshToken = this.refreshTokenSignal.asReadonly();
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly activeWorkspaceId = this.activeWorkspaceIdSignal.asReadonly();
  readonly authLoading = this.authLoadingSignal.asReadonly();
  readonly authError = this.authErrorSignal.asReadonly();
  readonly persistentSession = this.persistentSessionSignal.asReadonly();
  readonly authResolved = computed(() => this.authResolutionSignal() !== 'pending');

  readonly isAuthenticated = computed(
    () =>
      this.authResolutionSignal() === 'authenticated' &&
      Boolean(this.accessTokenSignal() && this.refreshTokenSignal() && this.currentUserSignal()),
  );
  readonly currentRole = computed(() => this.currentUserSignal()?.role ?? null);
  readonly permissions = computed(() => this.currentUserSignal()?.permissions ?? []);
  readonly displayName = computed(() => this.currentUserSignal()?.fullName ?? 'User');

  setSession(session: AuthSession, options?: { readonly persistent?: boolean }): void {
    const activeWorkspaceId =
      session.user.role === 'MASTER'
        ? this.activeWorkspaceIdSignal() ?? session.user.workspace.id
        : session.user.workspace.id;
    const persistent = options?.persistent ?? this.persistentSessionSignal();

    this.tokenStorage.setSession({
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
      accessTokenExpiresAt: session.accessTokenExpiresAt,
      refreshTokenExpiresAt: session.refreshTokenExpiresAt,
      activeWorkspaceId,
    }, { persistent });
    this.accessTokenSignal.set(session.accessToken);
    this.refreshTokenSignal.set(session.refreshToken);
    this.accessTokenExpiresAtSignal.set(session.accessTokenExpiresAt);
    this.refreshTokenExpiresAtSignal.set(session.refreshTokenExpiresAt);
    this.currentUserSignal.set(session.user);
    this.activeWorkspaceIdSignal.set(activeWorkspaceId);
    this.authErrorSignal.set(null);
    this.authResolutionSignal.set('authenticated');
    this.persistentSessionSignal.set(persistent);
  }

  patchCurrentUser(user: CurrentUser): void {
    this.currentUserSignal.set(user);
    if (user.role !== 'MASTER') {
      this.activeWorkspaceIdSignal.set(user.workspace.id);
    } else if (!this.activeWorkspaceIdSignal() && user.workspace.id) {
      this.activeWorkspaceIdSignal.set(user.workspace.id);
    }
    this.authResolutionSignal.set('authenticated');
    this.persist();
  }

  setActiveWorkspaceId(workspaceId: string | null): void {
    this.activeWorkspaceIdSignal.set(workspaceId);
    this.persist();
  }

  setAuthLoading(isLoading: boolean): void {
    this.authLoadingSignal.set(isLoading);
  }

  setAuthError(message: string | null): void {
    this.authErrorSignal.set(message);
  }

  beginSessionValidation(): void {
    if (this.accessTokenSignal() && this.refreshTokenSignal()) {
      this.authResolutionSignal.set('pending');
    }
  }

  markAnonymous(): void {
    this.authResolutionSignal.set('anonymous');
  }

  hasRestorableSession(): boolean {
    return Boolean(this.accessTokenSignal() && this.refreshTokenSignal());
  }

  hasValidAccessToken(): boolean {
    return this.hasValidExpiry(this.accessTokenExpiresAtSignal());
  }

  hasValidRefreshToken(): boolean {
    return this.hasValidExpiry(this.refreshTokenExpiresAtSignal());
  }

  clearSession(): void {
    this.tokenStorage.clear();
    this.accessTokenSignal.set(null);
    this.refreshTokenSignal.set(null);
    this.accessTokenExpiresAtSignal.set(null);
    this.refreshTokenExpiresAtSignal.set(null);
    this.currentUserSignal.set(null);
    this.activeWorkspaceIdSignal.set(null);
    this.authLoadingSignal.set(false);
    this.authErrorSignal.set(null);
    this.authResolutionSignal.set('anonymous');
    this.persistentSessionSignal.set(true);
  }

  hasAnyRole(roles: readonly UserRole[]): boolean {
    return hasAnyRole(this.currentRole(), roles);
  }

  hasPermission(permission: Permission): boolean {
    return hasPermission(this.permissions(), permission);
  }

  private persist(): void {
    const accessToken = this.accessTokenSignal();
    const refreshToken = this.refreshTokenSignal();
    const accessTokenExpiresAt = this.accessTokenExpiresAtSignal();
    const refreshTokenExpiresAt = this.refreshTokenExpiresAtSignal();

    if (!accessToken || !refreshToken || !accessTokenExpiresAt || !refreshTokenExpiresAt) {
      this.tokenStorage.clear();
      return;
    }

    this.tokenStorage.setSession({
      accessToken,
      refreshToken,
      accessTokenExpiresAt,
      refreshTokenExpiresAt,
      activeWorkspaceId: this.activeWorkspaceIdSignal(),
    }, { persistent: this.persistentSessionSignal() });
  }

  private hasValidExpiry(expiresAt: string | null): boolean {
    if (!expiresAt) {
      return false;
    }

    const parsed = Date.parse(expiresAt);
    if (Number.isNaN(parsed)) {
      return false;
    }

    return parsed - 30_000 > Date.now();
  }
}
