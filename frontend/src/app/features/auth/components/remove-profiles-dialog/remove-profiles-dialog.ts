import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { RememberedProfile } from '@app/features/auth/models/auth.models';
import { AvatarComponent } from '@app/shared/components/avatar/avatar';
import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-remove-profiles-dialog',
  standalone: true,
  imports: [AvatarComponent, IconComponent],
  templateUrl: './remove-profiles-dialog.html',
  styleUrl: './remove-profiles-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RemoveProfilesDialogComponent {
  readonly open = input(false);
  readonly profiles = input.required<readonly RememberedProfile[]>();
  readonly closed = output<void>();
  readonly removed = output<string>();
}
