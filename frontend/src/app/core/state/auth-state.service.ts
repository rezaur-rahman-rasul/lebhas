import { Injectable, computed, inject } from '@angular/core';

import { CurrentUserStore } from '../auth/current-user.store';
import { AuthSession } from '../auth/auth.types';
import { UserRole } from '@app/features/auth/models/user.models';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly store = inject(CurrentUserStore);

  readonly accessToken = this.store.accessToken;
  readonly currentUser = this.store.currentUser;
  readonly isAuthenticated = computed(() => this.store.isAuthenticated());
  readonly currentRole = computed(() => this.store.currentRole());
  readonly workspaceId = computed(() => this.store.activeWorkspaceId());

  hasAnyRole(roles: readonly UserRole[]): boolean {
    return this.store.hasAnyRole(roles);
  }

  setSession(session: AuthSession, options?: { readonly persistent?: boolean }): void {
    this.store.setSession(session, options);
  }

  clearSession(): void {
    this.store.clearSession();
  }
}
