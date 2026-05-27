import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ProfileEmptyStateComponent } from '../components/profile-empty-state/profile-empty-state';
import { ProfileLoadingStateComponent } from '../components/profile-loading-state/profile-loading-state';
import {
  SecurityActivityCardComponent,
  securityActivityLabel,
} from '../components/security-activity-card/security-activity-card';
import { SecurityActivityType } from '../models/profile.models';
import { ProfileStore } from '../state/profile.store';

@Component({
  selector: 'app-security-activity-page',
  standalone: true,
  imports: [
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    ProfileEmptyStateComponent,
    ProfileLoadingStateComponent,
    SecurityActivityCardComponent,
  ],
  templateUrl: './security-activity.html',
  styleUrl: './security-activity.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecurityActivityPage {
  private readonly router = inject(Router);
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(ProfileStore);

  protected readonly selectedType = signal<SecurityActivityType | string | null>(null);
  protected readonly activityTypes = Object.values(SecurityActivityType);
  protected readonly accessDenied = computed(() => !this.permissions.canViewSecurityActivity());
  protected readonly filteredActivity = computed(() => {
    const selectedType = this.selectedType();
    const activities = [...this.store.securityActivity()].sort(
      (a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt),
    );

    return selectedType
      ? activities.filter((activity) => activity.activityType === selectedType)
      : activities;
  });

  constructor() {
    effect(() => {
      if (!this.accessDenied()) {
        void this.store.loadSecurityActivity();
      }
    });
  }

  protected retry(): void {
    this.store.clearError();
    void this.store.loadSecurityActivity();
  }

  protected backToProfile(): void {
    void this.router.navigate(['/profile']);
  }

  protected typeLabel(type: SecurityActivityType | string): string {
    return securityActivityLabel(type);
  }

  protected setType(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedType.set(value || null);
  }
}
