import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';

@Component({
  selector: 'app-asset-context-banner',
  standalone: true,
  imports: [BadgeComponent],
  templateUrl: './asset-context-banner.html',
  styleUrl: './asset-context-banner.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetContextBannerComponent {
  readonly workspaceLabel = input('Workspace');
  readonly brandName = input('—');
  readonly productName = input('—');
  readonly projectName = input('—');
}
