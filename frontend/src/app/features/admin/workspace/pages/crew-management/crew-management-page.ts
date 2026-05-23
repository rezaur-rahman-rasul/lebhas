import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  CrewMember,
  CrewPermission,
  InviteCrewPayload,
  UpdateCrewMemberPayload,
} from '../../models/crew.models';
import { selectionRequiredValidator } from '../../workspace-form.validators';
import { WorkspaceStore } from '../../state/workspace.store';
import {
  InviteCrewFormValue,
  CrewInviteModalComponent,
} from '../../components/crew-invite-modal/crew-invite-modal';
import { CrewListComponent } from '../../components/crew-list/crew-list';
import { CrewPermissionPanelComponent } from '../../components/crew-permission-panel/crew-permission-panel';
import { WorkspaceSummaryCardComponent } from '../../components/workspace-summary-card/workspace-summary-card';

type EditControlName = 'status' | 'permissions';

@Component({
  selector: 'app-crew-management-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
    CrewInviteModalComponent,
    CrewListComponent,
    CrewPermissionPanelComponent,
    WorkspaceSummaryCardComponent,
  ],
  templateUrl: './crew-management-page.html',
  styleUrl: './crew-management-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CrewManagementPageComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  protected readonly store = inject(WorkspaceStore);
  protected readonly workspace = this.store.currentWorkspace;
  protected readonly crewMembers = this.store.crewMembers;
  protected readonly inviteOpen = signal(false);
  protected readonly editingMember = signal<CrewMember | null>(null);
  protected readonly inviteSaving = signal(false);
  protected readonly editSaving = signal(false);
  protected readonly removingCrewId = signal<string | null>(null);
  protected readonly inviteFieldErrors = signal<Readonly<Record<string, string>>>({});
  protected readonly editFieldErrors = signal<Readonly<Record<string, string>>>({});
  protected readonly editSubmitted = signal(false);
  protected readonly skeletonCards = Array.from({ length: 3 }, (_, index) => index);
  protected readonly canManage = computed(() => this.store.canManageCrew());
  protected readonly totalCrewLabel = computed(() => String(this.crewMembers().length));
  protected readonly activeCrewLabel = computed(
    () => String(this.crewMembers().filter((member) => member.status === 'ACTIVE').length),
  );
  protected readonly invitedCrewLabel = computed(
    () => String(this.crewMembers().filter((member) => member.status === 'INVITED').length),
  );
  protected readonly pageDescription = computed(() =>
    this.canManage()
      ? 'Invite crew members, review access status, and keep permission scope aligned to workspace operations.'
      : 'Crew visibility is limited in this session.',
  );
  protected readonly editForm = this.formBuilder.group({
    status: this.formBuilder.control<UpdateCrewMemberPayload['status']>('ACTIVE', {
      validators: [Validators.required],
    }),
    permissions: this.formBuilder.control<readonly CrewPermission[]>(['WORKSPACE_VIEW'], {
      validators: [selectionRequiredValidator()],
    }),
  });

  constructor() {
    void this.store.loadCrewContext();
  }

  protected closeInvite(): void {
    this.inviteOpen.set(false);
    this.inviteFieldErrors.set({});
  }

  protected async inviteCrew(value: InviteCrewFormValue): Promise<void> {
    this.inviteSaving.set(true);
    this.inviteFieldErrors.set({});

    const payload: InviteCrewPayload = {
      email: value.email.trim(),
      role: 'CREW',
      permissions: value.permissions,
    };

    const result = await this.store.inviteCrew(payload);
    if (result.ok) {
      this.inviteOpen.set(false);
    } else {
      this.inviteFieldErrors.set(result.fieldErrors);
    }

    this.inviteSaving.set(false);
  }

  protected openEdit(member: CrewMember): void {
    this.editingMember.set(member);
    this.editSubmitted.set(false);
    this.editFieldErrors.set({});
    this.editForm.reset(
      {
        status: member.status === 'SUSPENDED' ? 'SUSPENDED' : 'ACTIVE',
        permissions: [...member.permissions],
      },
      { emitEvent: false },
    );
  }

  protected closeEdit(): void {
    this.editingMember.set(null);
    this.editFieldErrors.set({});
  }

  protected updateEditPermissions(permissions: readonly CrewPermission[]): void {
    this.editForm.controls.permissions.setValue(permissions);
    this.editForm.controls.permissions.markAsTouched();
    this.editForm.controls.permissions.updateValueAndValidity();
  }

  protected async saveEdit(): Promise<void> {
    const member = this.editingMember();
    if (!member) {
      return;
    }

    this.editSubmitted.set(true);
    this.editForm.markAllAsTouched();
    this.editFieldErrors.set({});

    if (this.editForm.invalid) {
      return;
    }

    this.editSaving.set(true);

    const value = this.editForm.getRawValue();
    const payload: UpdateCrewMemberPayload = {
      status: value.status,
      permissions: value.permissions,
    };

    const result = await this.store.updateCrewMember(member.id, payload);
    if (result.ok) {
      this.closeEdit();
    } else {
      this.editFieldErrors.set(result.fieldErrors);
    }

    this.editSaving.set(false);
  }

  protected async removeCrew(member: CrewMember): Promise<void> {
    this.removingCrewId.set(member.id);
    await this.store.removeCrewMember(member.id);
    this.removingCrewId.set(null);
  }

  protected memberName(member: CrewMember): string {
    const fullName = [member.firstName, member.lastName].filter(Boolean).join(' ').trim();
    return fullName || member.email;
  }

  protected editFieldError(controlName: EditControlName): string {
    const control = this.editForm.controls[controlName];
    const serverError = this.editFieldErrors()[controlName];

    if (serverError) {
      return serverError;
    }

    if (!this.editSubmitted() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'This field is required.';
    }

    if (control.hasError('selectionRequired')) {
      return 'Select at least one permission.';
    }

    return '';
  }

  protected selectClasses(): string {
    return 'h-11 w-full rounded-md border border-border bg-white px-3 text-sm text-ink shadow-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
  }
}
