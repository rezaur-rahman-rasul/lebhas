import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { UserSessionView } from '../../models/profile.models';

@Component({
  selector: 'app-session-card',
  standalone: true,
  imports: [ButtonComponent, CardComponent, IconComponent],
  templateUrl: './session-card.html',
  styleUrl: './session-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionCardComponent {
  readonly session = input.required<UserSessionView>();
  readonly revoking = input(false);
  readonly allowRevoke = input(true);

  readonly revokeSession = output<string>();

  protected readonly title = computed(() => (this.session().current ? 'Current session' : 'Active session'));
  protected readonly canRevoke = computed(() => this.allowRevoke() && !this.session().current);
}
