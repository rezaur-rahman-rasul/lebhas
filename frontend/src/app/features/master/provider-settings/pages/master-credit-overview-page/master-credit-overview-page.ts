import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { WorkspaceCreditAdjustmentDialogComponent } from '../../components/workspace-credit-adjustment-dialog/workspace-credit-adjustment-dialog';
import { WorkspaceCreditAccountView, WorkspaceCreditAdjustmentRequest } from '../../models/provider-credit-exchange.models';
import { ProviderCreditsStore } from '../../state/providerCredits.store';

type WorkspaceCreditRow = WorkspaceCreditAccountView & {
  readonly workspaceName?: string | null;
  readonly lastActivityAt?: string | null;
};

@Component({ selector: 'app-master-credit-overview-page', standalone: true, imports: [DatePipe, DecimalPipe, ButtonComponent, AppErrorStateComponent, PageHeaderComponent, WorkspaceCreditAdjustmentDialogComponent], templateUrl: './master-credit-overview-page.html', styleUrl: './master-credit-overview-page.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class MasterCreditOverviewPage {
  protected readonly credits = inject(ProviderCreditsStore);
  protected readonly selectedWorkspace = signal<WorkspaceCreditRow | null>(null);
  protected readonly adjustmentOpen = signal(false);
  protected readonly lowBalanceProviderCount = computed<number>(() => {
    const value: unknown = this.credits.masterCreditOverview()?.lowBalanceProviders;
    if (Array.isArray(value)) {
      return value.length;
    }
    return typeof value === 'number' ? value : 0;
  });

  constructor() { effect(() => void this.credits.loadOverview()); }

  protected openAdjustment(workspace: WorkspaceCreditRow): void {
    this.selectedWorkspace.set(workspace);
    this.adjustmentOpen.set(true);
  }

  protected adjustWorkspace(payload: WorkspaceCreditAdjustmentRequest): void {
    const workspace = this.selectedWorkspace();
    if (!workspace) {
      return;
    }

    void this.credits.adjustWorkspaceCredits(workspace.workspaceId, payload).then((ok) => {
      if (ok) {
        this.adjustmentOpen.set(false);
        this.selectedWorkspace.set(null);
      }
    });
  }

  protected closeAdjustment(): void {
    this.adjustmentOpen.set(false);
    this.selectedWorkspace.set(null);
  }
}
