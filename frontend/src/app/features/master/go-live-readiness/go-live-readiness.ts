import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { IconComponent } from '@app/shared/components/icon/icon';
import { normalizeHttpError } from '@app/core/api/http-error';
import { GoLiveReadinessApiService, GoLiveReadinessCheck, ReadinessStatus } from './go-live-readiness.service';

type BadgeTone = 'neutral' | 'brand' | 'blue' | 'red';

@Component({
  selector: 'app-go-live-readiness',
  standalone: true,
  imports: [RouterLink, CardComponent, AppErrorStateComponent, PageHeaderComponent, BadgeComponent, IconComponent],
  templateUrl: './go-live-readiness.html',
  styleUrl: './go-live-readiness.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GoLiveReadinessPage {
  private readonly api = inject(GoLiveReadinessApiService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly checks = signal<readonly GoLiveReadinessCheck[]>([]);
  protected readonly readyCount = signal(0);
  protected readonly attentionCount = signal(0);
  protected readonly blockedCount = signal(0);

  constructor() {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await this.api.getReadiness();
      this.checks.set(data.checks);
      this.readyCount.set(data.ready);
      this.attentionCount.set(data.needsAttention);
      this.blockedCount.set(data.blocked);
    } catch (error) {
      this.error.set(normalizeHttpError(error).message || 'Go-live readiness could not be loaded.');
      this.checks.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  protected statusLabel(status: ReadinessStatus): string {
    return status === 'READY' ? 'Ready' : status === 'BLOCKED' ? 'Blocked' : 'Needs Attention';
  }

  protected statusTone(status: ReadinessStatus): BadgeTone {
    return status === 'READY' ? 'brand' : status === 'BLOCKED' ? 'red' : 'blue';
  }
}
