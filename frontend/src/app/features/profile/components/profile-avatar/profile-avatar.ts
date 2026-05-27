import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { UserProfileView } from '../../models/profile.models';
import { getInitials, safeProfileImageUrl } from '../../services/profile-security';

type ProfileAvatarSize = 'sm' | 'md' | 'lg' | 'xl';

@Component({
  selector: 'app-profile-avatar',
  standalone: true,
  templateUrl: './profile-avatar.html',
  styleUrl: './profile-avatar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileAvatarComponent {
  readonly profile = input<UserProfileView | null>(null);
  readonly imageUrl = input<string | null>(null);
  readonly initials = input<string | null>(null);
  readonly displayName = input<string | null>(null);
  readonly size = input<ProfileAvatarSize>('lg');
  readonly loading = input(false);

  protected readonly safeImageUrl = computed(() => {
    const explicitUrl = this.imageUrl()?.trim();
    if (explicitUrl) {
      return explicitUrl.startsWith('https://') || explicitUrl.startsWith('/') ? explicitUrl : null;
    }

    return safeProfileImageUrl(this.profile());
  });

  protected readonly fallbackInitials = computed(
    () => this.initials()?.trim() || getInitials(this.profile()) || 'U',
  );
  protected readonly label = computed(() => {
    const profile = this.profile();
    const name =
      this.displayName()?.trim() ||
      profile?.displayName ||
      [profile?.firstName, profile?.lastName].filter(Boolean).join(' ') ||
      'User';

    return `${name} profile photo`;
  });
  protected readonly avatarClasses = computed(() => {
    const sizes: Record<ProfileAvatarSize, string> = {
      sm: 'size-10 text-sm',
      md: 'size-14 text-base',
      lg: 'size-20 text-2xl',
      xl: 'size-28 text-3xl',
    };

    return [
      'profile-avatar grid shrink-0 place-items-center overflow-hidden rounded-full border border-border bg-panel font-semibold text-brand-200 ring-1 ring-white/10',
      sizes[this.size()],
      this.loading() ? 'animate-pulse' : '',
    ]
      .filter(Boolean)
      .join(' ');
  });
}
