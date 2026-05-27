import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { roleBadgeTone } from '@app/core/auth/permissions';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { AvatarComponent } from '@app/shared/components/avatar/avatar';
import { IconComponent } from '@app/shared/components/icon/icon';
import { AuthFacade } from '@app/features/auth/services/auth.facade';
import { ProfileStore } from '@app/features/profile/state/profile.store';

@Component({
  selector: 'app-user-profile-dropdown',
  standalone: true,
  imports: [AvatarComponent, BadgeComponent, IconComponent],
  templateUrl: './user-profile-dropdown.html',
  styleUrl: './user-profile-dropdown.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserProfileDropdownComponent {
  protected readonly userStore = inject(CurrentUserStore);
  protected readonly workspaceStore = inject(WorkspaceStore);
  protected readonly profileStore = inject(ProfileStore);
  private readonly authFacade = inject(AuthFacade);
  private readonly router = inject(Router);
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly open = signal(false);
  protected readonly badgeTone = computed(() => roleBadgeTone(this.userStore.currentRole()));
  protected readonly workspaceName = this.workspaceStore.workspaceLabel;
  protected readonly displayName = computed(() => this.profileStore.displayName() || this.userStore.displayName() || 'User');
  protected readonly displayEmail = computed(() => this.userStore.currentUser()?.email || 'Email unavailable');
  protected readonly avatarUrl = this.profileStore.profileImageUrl;

  constructor() {
    effect(() => {
      if (!this.userStore.isAuthenticated() || this.profileStore.profile()) {
        return;
      }

      void this.profileStore.loadMyProfile();
    });
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.open.set(false);
  }

  protected async logout(): Promise<void> {
    this.open.set(false);
    await this.authFacade.logout({ notify: true });
  }

  protected async goToProfile(): Promise<void> {
    await this.navigateTo('/profile');
  }

  protected async goToSettings(): Promise<void> {
    await this.navigateTo('/profile/settings');
  }

  protected async goToSecurityActivity(): Promise<void> {
    await this.navigateTo('/profile/security');
  }

  private async navigateTo(route: string): Promise<void> {
    this.open.set(false);
    await this.router.navigateByUrl(route);
  }
}
