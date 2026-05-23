import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import { UpdateBrandProfilePayload } from '../../models/brand-profile.models';
import { WorkspaceStore } from '../../state/workspace.store';
import { BrandProfileFormComponent } from '../../components/brand-profile-form/brand-profile-form';

@Component({
  selector: 'app-brand-profile-page',
  standalone: true,
  imports: [BadgeComponent, EmptyStateComponent, PageHeaderComponent, BrandProfileFormComponent],
  templateUrl: './brand-profile-page.html',
  styleUrl: './brand-profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandProfilePageComponent {
  protected readonly store = inject(WorkspaceStore);
  protected readonly profile = this.store.brandProfile;
  protected readonly saving = signal(false);
  protected readonly fieldErrors = signal<Readonly<Record<string, string>>>({});
  protected readonly canEdit = computed(() => this.store.canEditBrandProfile());
  protected readonly pageDescription = computed(() =>
    this.canEdit()
      ? 'Capture the brand inputs that future AI creative generation and review workflows will use.'
      : 'Brand profile context is visible here for review, but this session cannot update it.',
  );

  constructor() {
    void this.store.loadBrandProfileContext();
  }

  protected async save(payload: UpdateBrandProfilePayload): Promise<void> {
    this.fieldErrors.set({});
    this.saving.set(true);

    const result = await this.store.saveBrandProfile(payload);
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
      next[field === 'preferredCta' ? 'preferredCTA' : field] = message;
    }

    return next;
  }
}
