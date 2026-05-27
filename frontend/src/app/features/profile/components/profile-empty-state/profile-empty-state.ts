import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-profile-empty-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './profile-empty-state.html',
  styleUrl: './profile-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileEmptyStateComponent {
  readonly title = input('Nothing to show yet');
  readonly message = input('Profile details will appear here once available.');
  readonly icon = input('user-circle');
}
