import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  DEFAULT_PROMPT_TEMPLATE_FILTERS,
  PLATFORM_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
  PROMPT_TEMPLATE_STATUS_OPTIONS,
  PromptLanguage,
  PromptPlatform,
  PromptTemplate,
  PromptTemplateFilter,
  PromptTemplatePayload,
  PromptTemplateStatus,
} from '../../models/prompt.models';
import { PromptStore } from '../../state/prompt.store';
import { PromptTemplateCard } from '../../components/prompt-template-card/prompt-template-card';
import { PromptTemplateForm } from '../../components/prompt-template-form/prompt-template-form';

@Component({
  selector: 'app-prompt-templates-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
    PromptTemplateCard,
    PromptTemplateForm,
  ],
  templateUrl: './prompt-templates.html',
  styleUrl: './prompt-templates.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptTemplatesPage {
  protected readonly store = inject(PromptStore);
  private readonly router = inject(Router);

  private readonly formOpenSignal = signal(false);
  private readonly deleteOpenSignal = signal(false);
  private readonly editingTemplateSignal = signal<PromptTemplate | null>(null);
  private readonly pendingDeleteTemplateSignal = signal<PromptTemplate | null>(null);
  private readonly fieldErrorsSignal = signal<Readonly<Record<string, string>>>({});

  protected readonly filterForm = new FormGroup({
    search: new FormControl('', { nonNullable: true }),
    platform: new FormControl<PromptPlatform | ''>('', { nonNullable: true }),
    language: new FormControl<PromptLanguage | ''>('', { nonNullable: true }),
    status: new FormControl<PromptTemplateStatus | ''>('', { nonNullable: true }),
  });

  protected readonly formOpen = this.formOpenSignal.asReadonly();
  protected readonly deleteOpen = this.deleteOpenSignal.asReadonly();
  protected readonly editingTemplate = this.editingTemplateSignal.asReadonly();
  protected readonly pendingDeleteTemplate = this.pendingDeleteTemplateSignal.asReadonly();
  protected readonly fieldErrors = this.fieldErrorsSignal.asReadonly();

  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS;
  protected readonly statusOptions = PROMPT_TEMPLATE_STATUS_OPTIONS;
  protected readonly templateCountLabel = computed(
    () => `${this.store.templates().length} template${this.store.templates().length === 1 ? '' : 's'}`,
  );

  constructor() {
    void this.store.loadTemplates();
  }

  protected openCreate(): void {
    this.editingTemplateSignal.set(null);
    this.fieldErrorsSignal.set({});
    this.formOpenSignal.set(true);
  }

  protected openEdit(template: PromptTemplate): void {
    this.editingTemplateSignal.set(template);
    this.fieldErrorsSignal.set({});
    this.formOpenSignal.set(true);
  }

  protected closeForm(): void {
    this.formOpenSignal.set(false);
    this.editingTemplateSignal.set(null);
    this.fieldErrorsSignal.set({});
  }

  protected async saveTemplate(payload: PromptTemplatePayload): Promise<void> {
    this.fieldErrorsSignal.set({});

    const result = this.editingTemplate()
      ? await this.store.updateTemplate(this.editingTemplate()!.id, payload)
      : await this.store.createTemplate(payload);

    if (!result.ok) {
      this.fieldErrorsSignal.set(result.fieldErrors);
      return;
    }

    this.closeForm();
  }

  protected confirmDelete(template: PromptTemplate): void {
    this.pendingDeleteTemplateSignal.set(template);
    this.deleteOpenSignal.set(true);
  }

  protected cancelDelete(): void {
    this.pendingDeleteTemplateSignal.set(null);
    this.deleteOpenSignal.set(false);
  }

  protected async deleteTemplate(): Promise<void> {
    const template = this.pendingDeleteTemplate();
    if (!template) {
      return;
    }

    const result = await this.store.deleteTemplate(template.id);
    if (!result.ok) {
      return;
    }

    this.cancelDelete();
  }

  protected async applyFilters(): Promise<void> {
    await this.store.loadTemplates(this.buildFilters());
  }

  protected async resetFilters(): Promise<void> {
    this.filterForm.reset(
      {
        search: '',
        platform: '',
        language: '',
        status: '',
      },
      { emitEvent: false },
    );
    await this.store.loadTemplates(DEFAULT_PROMPT_TEMPLATE_FILTERS);
  }

  protected useTemplate(template: PromptTemplate): void {
    this.store.selectTemplate(template);
    void this.router.navigate(['/admin/prompts']);
  }

  protected reloadTemplates(): void {
    void this.store.loadTemplates();
  }

  private buildFilters(): PromptTemplateFilter {
    const value = this.filterForm.getRawValue();
    return {
      ...DEFAULT_PROMPT_TEMPLATE_FILTERS,
      search: value.search.trim(),
      platform: value.platform || null,
      language: value.language || null,
      status: value.status || null,
    };
  }
}
