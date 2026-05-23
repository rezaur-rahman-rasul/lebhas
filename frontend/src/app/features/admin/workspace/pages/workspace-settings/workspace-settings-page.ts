import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  DEFAULT_WORKSPACE_NOTIFICATION_PREFERENCES,
  WorkspaceSettingsFormValue,
} from '../../models/workspace.models';
import { nullIfBlank } from '../../workspace-form.validators';
import { WorkspaceStore } from '../../state/workspace.store';
import { WorkspaceSettingsFormComponent } from '../../components/workspace-settings-form/workspace-settings-form';

@Component({
  selector: 'app-workspace-settings-page',
  standalone: true,
  imports: [
    BadgeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    WorkspaceSettingsFormComponent,
  ],
  templateUrl: './workspace-settings-page.html',
  styleUrl: './workspace-settings-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceSettingsPageComponent {
  protected readonly store = inject(WorkspaceStore);
  protected readonly workspace = this.store.currentWorkspace;
  protected readonly settings = this.store.workspaceSettings;
  protected readonly saving = signal(false);
  protected readonly fieldErrors = signal<Readonly<Record<string, string>>>({});
  protected readonly canEdit = computed(() => this.store.canEditWorkspace());
  protected readonly pageDescription = computed(() =>
    this.canEdit()
      ? 'Update the workspace identity, crew policy toggles, and default operating preferences.'
      : 'Workspace settings are visible here for oversight, but this session cannot change them.',
  );

  constructor() {
    void this.store.loadWorkspaceSettingsContext();
  }

  protected async save(value: WorkspaceSettingsFormValue): Promise<void> {
    const workspace = this.workspace();
    const settings = this.settings();
    if (!workspace || !settings) {
      return;
    }

    this.fieldErrors.set({});
    this.saving.set(true);

    const result = await this.store.saveWorkspaceSettings({
      workspace: {
        name: value.name.trim(),
        slug: workspace.slug,
        logoUrl: nullIfBlank(value.logoUrl),
        description: nullIfBlank(value.description),
        industry: nullIfBlank(value.industry),
        timezone: value.timezone.trim(),
        language: value.language,
        currency: value.currency.trim().toUpperCase(),
        country: value.country.trim().toUpperCase(),
        status: workspace.status,
      },
      settings: {
        allowCrewDownload: value.allowCrewDownload,
        allowCrewPublish: value.allowCrewPublish,
        defaultLanguage: value.language,
        defaultTimezone: value.timezone.trim(),
        notificationPreferences:
          settings.notificationPreferences ?? DEFAULT_WORKSPACE_NOTIFICATION_PREFERENCES,
        workspaceVisibility: value.workspaceVisibility,
      },
    });

    if (!result.ok) {
      this.fieldErrors.set(this.mapFieldErrors(result.fieldErrors));
    }

    this.saving.set(false);
  }

  private mapFieldErrors(
    fieldErrors: Readonly<Record<string, string>>,
  ): Readonly<Record<string, string>> {
    const next: Record<string, string> = {};

    for (const [field, message] of Object.entries(fieldErrors)) {
      if (field === 'defaultLanguage') {
        next['language'] = message;
      } else if (field === 'defaultTimezone') {
        next['timezone'] = message;
      } else {
        next[field] = message;
      }
    }

    return next;
  }
}
