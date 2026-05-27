import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { ModalShellComponent } from '@app/shared/components/app-confirm-dialog/app-confirm-dialog';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ProfileEmptyStateComponent } from '../components/profile-empty-state/profile-empty-state';
import { ProfileLoadingStateComponent } from '../components/profile-loading-state/profile-loading-state';
import { SessionCardComponent } from '../components/session-card/session-card';
import { UserSessionView } from '../models/profile.models';
import { ProfileStore } from '../state/profile.store';

@Component({
  selector: 'app-user-sessions-page',
  standalone: true,
  imports: [
    ButtonComponent,
    CardComponent,
    ModalShellComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    ProfileEmptyStateComponent,
    ProfileLoadingStateComponent,
    SessionCardComponent,
  ],
  templateUrl: './user-sessions.html',
  styleUrl: './user-sessions.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserSessionsPage {
  private readonly router = inject(Router);
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(ProfileStore);

  protected readonly selectedSession = signal<UserSessionView | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly accessDenied = computed(() => !this.permissions.canViewSecurityActivity());
  protected readonly hasSessions = computed(() => this.store.sessions().length > 0);
  protected readonly confirmOpen = computed(() => Boolean(this.selectedSession()));

  constructor() {
    effect(() => {
      if (!this.accessDenied()) {
        void this.store.loadSessions();
      }
    });
  }

  protected retry(): void {
    this.store.clearError();
    void this.store.loadSessions();
  }

  protected backToProfile(): void {
    void this.router.navigate(['/profile']);
  }

  protected requestRevoke(sessionId: string): void {
    const session = this.store.sessions().find((item) => item.sessionId === sessionId) ?? null;
    this.selectedSession.set(session);
  }

  protected closeConfirm(): void {
    this.selectedSession.set(null);
  }

  protected async confirmRevoke(): Promise<void> {
    const session = this.selectedSession();
    if (!session) {
      return;
    }

    const result = await this.store.revokeSession(session.sessionId);
    if (result.ok) {
      this.successMessage.set('Session revoked successfully.');
      this.selectedSession.set(null);
    }
  }
}
