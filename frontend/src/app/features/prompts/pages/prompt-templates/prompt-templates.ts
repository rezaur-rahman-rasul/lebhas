import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';

@Component({
  selector: 'app-prompt-templates-page',
  standalone: true,
  imports: [RouterLink, BadgeComponent, EmptyStateComponent, PageHeaderComponent],
  templateUrl: './prompt-templates.html',
  styleUrl: './prompt-templates.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptTemplatesPage {
  private readonly permissions = inject(PermissionStore);

  protected readonly canViewPromptTemplates = this.permissions.canViewPromptTemplates;
  protected readonly canManageTemplates = this.permissions.canManageTemplates;
}
