import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';

import { normalizeHttpError } from '@app/core/api/http-error';
import { AuthService } from '@app/core/auth/auth.service';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { AuthActionFailure, AuthActionResult, AuthSession } from '@app/core/auth/auth.types';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
} from '../models/auth.models';
import { RememberedProfilesStorage } from './remembered-profiles.storage';

@Injectable({ providedIn: 'root' })
export class AuthFacade {
  private readonly authService = inject(AuthService);
  private readonly currentUserStore = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly router = inject(Router);
  private readonly rememberedProfiles = inject(RememberedProfilesStorage);

  private initialized = false;
  private initializeInFlight: Promise<void> | null = null;
  private refreshInFlight: Promise<boolean> | null = null;

  readonly currentUser = this.currentUserStore.currentUser;
  readonly isAuthenticated = this.currentUserStore.isAuthenticated;
  readonly currentRole = this.currentUserStore.currentRole;
  readonly activeWorkspaceId = this.currentUserStore.activeWorkspaceId;
  readonly authLoading = this.currentUserStore.authLoading;
  readonly authError = this.currentUserStore.authError;
  readonly accessToken = this.currentUserStore.accessToken;
  readonly refreshToken = this.currentUserStore.refreshToken;

  async initialize(): Promise<void> {
    if (this.initialized) {
      return;
    }

    if (this.initializeInFlight) {
      return this.initializeInFlight;
    }

    this.currentUserStore.beginSessionValidation();
    this.initializeInFlight = this.restoreSession().finally(() => {
      this.initialized = true;
      this.initializeInFlight = null;
    });

    return this.initializeInFlight;
  }

  private async restoreSession(): Promise<void> {
    if (!this.currentUserStore.hasRestorableSession()) {
      this.currentUserStore.markAnonymous();
      return;
    }

    if (!this.currentUserStore.hasValidRefreshToken()) {
      this.currentUserStore.clearSession();
      return;
    }

    if (!this.currentUserStore.hasValidAccessToken()) {
      await this.tryRefresh({ redirectOnFailure: false, toastOnFailure: false });
      return;
    }

    try {
      const currentUser = await this.authService.getCurrentUser();
      this.currentUserStore.patchCurrentUser(currentUser);
      this.rememberedProfiles.rememberUser(currentUser);
    } catch {
      const refreshed = await this.tryRefresh({ redirectOnFailure: false, toastOnFailure: false });
      if (!refreshed) {
        this.currentUserStore.clearSession();
      }
    }
  }

  async login(
    payload: LoginRequest,
    returnUrl?: string,
    options?: { readonly rememberMe?: boolean },
  ): Promise<AuthActionResult> {
    return this.runSessionAction(
      () => this.authService.login(payload),
      async (session) => {
        this.currentUserStore.setSession(session, {
          persistent: options?.rememberMe ?? true,
        });
        await this.router.navigateByUrl(returnUrl || '/dashboard');
      },
    );
  }

  async register(payload: RegisterRequest): Promise<AuthActionResult> {
    return this.runSessionAction(
      () => this.authService.register(payload),
      async (session) => {
      this.currentUserStore.setSession(session, { persistent: true });
      this.notifications.success('Workspace created', 'Your account is ready.');
      await this.router.navigateByUrl('/dashboard');
    },
    );
  }

  async acceptInvite(payload: RegisterRequest): Promise<AuthActionResult> {
    return this.runSessionAction(
      () => this.authService.register(payload),
      async (session) => {
      this.currentUserStore.setSession(session, { persistent: true });
      this.notifications.success('Invitation accepted', 'You can start working immediately.');
      await this.router.navigateByUrl('/dashboard');
    },
    );
  }

  async logout(options?: { readonly redirectTo?: string; readonly notify?: boolean }): Promise<void> {
    const refreshToken = this.currentUserStore.refreshToken();

    this.currentUserStore.setAuthLoading(true);
    this.currentUserStore.setAuthError(null);

    try {
      if (refreshToken) {
        await this.authService.logout({ refreshToken });
      }
    } catch {
      // Local session teardown still needs to complete.
    } finally {
      this.currentUserStore.clearSession();
      if (options?.notify) {
        this.notifications.info('Signed out', 'Your session has been closed.');
      }
      this.currentUserStore.setAuthLoading(false);
      await this.router.navigateByUrl(options?.redirectTo ?? '/');
    }
  }

  async tryRefresh(options?: {
    readonly redirectOnFailure?: boolean;
    readonly toastOnFailure?: boolean;
  }): Promise<boolean> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }

    const refreshToken = this.currentUserStore.refreshToken();
    if (!refreshToken || !this.currentUserStore.hasValidRefreshToken()) {
      this.handleExpiredSession(options);
      return false;
    }

    this.refreshInFlight = this.performRefresh({ refreshToken }, options).finally(() => {
      this.refreshInFlight = null;
    });

    return this.refreshInFlight;
  }

  private async performRefresh(
    payload: RefreshTokenRequest,
    options?: {
      readonly redirectOnFailure?: boolean;
      readonly toastOnFailure?: boolean;
    },
  ): Promise<boolean> {
    try {
      const session = await this.authService.refreshSession(payload);
      this.currentUserStore.setSession(session, {
        persistent: this.currentUserStore.persistentSession(),
      });
      this.rememberedProfiles.rememberUser(session.user);
      return true;
    } catch {
      this.handleExpiredSession(options);
      return false;
    }
  }

  private async runSessionAction(
    action: () => Promise<AuthSession>,
    onSuccess: (session: AuthSession) => Promise<void>,
  ): Promise<AuthActionResult> {
    this.currentUserStore.setAuthLoading(true);
    this.currentUserStore.setAuthError(null);

    try {
      const session = await action();
      this.rememberedProfiles.rememberUser(session.user);
      await onSuccess(session);
      return { ok: true, data: undefined };
    } catch (error) {
      const failure = this.toActionFailure(error);
      this.currentUserStore.setAuthError(failure.message);
      return failure;
    } finally {
      this.currentUserStore.setAuthLoading(false);
    }
  }

  private toActionFailure(error: unknown): AuthActionFailure {
    const normalized = normalizeHttpError(error);
    return {
      ok: false,
      status: normalized.status,
      message: normalized.message,
      errors: normalized.errors,
      fieldErrors: normalized.errors.reduce<Record<string, string>>((accumulator, item) => {
        if (item.field) {
          accumulator[item.field] = item.message;
        }
        return accumulator;
      }, {}),
    };
  }

  private handleExpiredSession(options?: {
    readonly redirectOnFailure?: boolean;
    readonly toastOnFailure?: boolean;
  }): void {
    this.currentUserStore.clearSession();

    if (options?.toastOnFailure ?? true) {
      this.notifications.error('Session expired', 'Please sign in again.');
    }

    if (options?.redirectOnFailure ?? true) {
      void this.navigateToLoginSurface(this.router.url);
    }
  }

  private async navigateToLoginSurface(returnUrl?: string): Promise<void> {
    const nextReturnUrl =
      returnUrl && returnUrl !== '/' && !returnUrl.startsWith('/login')
        ? returnUrl
        : undefined;

    await this.router.navigate(['/login'], {
      queryParams: {
        ...(nextReturnUrl ? { returnUrl: nextReturnUrl } : {}),
      },
    });
  }
}
