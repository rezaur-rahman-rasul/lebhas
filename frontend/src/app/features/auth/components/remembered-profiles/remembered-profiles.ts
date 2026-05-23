import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { RememberedProfile } from '@app/features/auth/models/auth.models';
import { AvatarComponent } from '@app/shared/components/avatar/avatar';
import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-remembered-profiles',
  standalone: true,
  imports: [RouterLink, AvatarComponent, IconComponent],
  templateUrl: './remembered-profiles.html',
  styleUrl: './remembered-profiles.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RememberedProfilesComponent {
  readonly profiles = input.required<readonly RememberedProfile[]>();
  readonly loading = input(false);
  readonly profileSelected = output<RememberedProfile>();
  readonly useAnotherProfile = output<void>();
  readonly manageProfiles = output<void>();
}
