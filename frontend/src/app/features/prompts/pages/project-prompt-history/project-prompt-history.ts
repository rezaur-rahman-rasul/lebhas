import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';

@Component({
  selector: 'app-project-prompt-history-page',
  standalone: true,
  imports: [RouterLink, BadgeComponent, EmptyStateComponent, PageHeaderComponent],
  templateUrl: './project-prompt-history.html',
  styleUrl: './project-prompt-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectPromptHistoryPage {
  private readonly route = inject(ActivatedRoute);
  private readonly permissions = inject(PermissionStore);

  protected readonly projectId = computed(() => this.route.snapshot.paramMap.get('projectId') ?? '');
  protected readonly canViewPromptHistory = this.permissions.canViewPromptHistory;
}
