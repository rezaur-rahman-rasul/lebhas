import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { roleBadgeTone } from '@app/core/auth/permissions';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { AvatarComponent } from '@app/shared/components/avatar/avatar';
import { IconComponent } from '@app/shared/components/icon/icon';
import { AuthFacade } from '@app/features/auth/services/auth.facade';

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
  private readonly authFacade = inject(AuthFacade);
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly open = signal(false);
  protected readonly badgeTone = computed(() => roleBadgeTone(this.userStore.currentRole()));
  protected readonly workspaceName = this.workspaceStore.workspaceLabel;

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }

  protected async logout(): Promise<void> {
    this.open.set(false);
    await this.authFacade.logout({ notify: true });
  }
}
