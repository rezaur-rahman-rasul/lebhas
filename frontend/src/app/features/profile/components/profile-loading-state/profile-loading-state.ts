import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-profile-loading-state',
  standalone: true,
  templateUrl: './profile-loading-state.html',
  styleUrl: './profile-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileLoadingStateComponent {
  readonly label = input('Loading profile information');
  readonly cardCount = input(3);
  readonly rowCount = input(4);

  protected readonly skeletonCards = Array.from({ length: 6 }, (_, index) => index);
  protected readonly skeletonRows = Array.from({ length: 8 }, (_, index) => index);
}
