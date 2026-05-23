import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ProjectStore } from '@app/features/projects/project.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import { AssetStore } from '../../state/asset.store';

@Component({
  selector: 'app-asset-library-page',
  standalone: true,
  imports: [
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    IconComponent,
    PageHeaderComponent,
  ],
  templateUrl: './asset-library.html',
  styleUrl: './asset-library.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetLibraryPage {
  protected readonly store = inject(AssetStore);
  private readonly auth = inject(CurrentUserStore);
  private readonly workspace = inject(WorkspaceStore);
  private readonly projectStore = inject(ProjectStore);
  private readonly router = inject(Router);

  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly projects = this.projectStore.items;
  protected readonly workspaceLabel = this.workspace.workspaceLabel;

  constructor() {
    void this.initialize();
  }

  protected openProjectAssets(projectId: string): void {
    void this.router.navigate(['/assets/projects', projectId]);
  }

  private async initialize(): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.projectStore.load(workspaceId);
  }
}
