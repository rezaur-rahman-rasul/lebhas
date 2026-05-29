import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  ConfigureLayerRoutingPolicyPayload,
  CreativePipelineView,
} from './creative-pipeline.models';
import { CreativePipelineService } from './creative-pipeline.service';

@Injectable({ providedIn: 'root' })
export class CreativePipelineStore {
  private readonly service = inject(CreativePipelineService);
  private readonly notifications = inject(NotificationStateService);

  private readonly pipelinesSignal = signal<readonly CreativePipelineView[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly pipelines = this.pipelinesSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly activePipelines = computed(() => this.pipelinesSignal().filter((pipeline) => pipeline.active));
  readonly layers = computed(() => this.pipelinesSignal().flatMap((pipeline) => pipeline.layers));

  async load(): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      this.pipelinesSignal.set(await this.service.listPipelines());
    } catch (error) {
      this.errorSignal.set(
        normalizeHttpError(error).message || 'Creative pipeline configuration could not load.',
      );
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async configureRoutingPolicy(
    pipelineId: string,
    layerId: string,
    payload: ConfigureLayerRoutingPolicyPayload,
  ): Promise<void> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const pipeline = await this.service.configureRoutingPolicy(pipelineId, layerId, payload);
      this.pipelinesSignal.update((pipelines) =>
        pipelines.map((item) => (item.id === pipeline.id ? pipeline : item)),
      );
      this.notifications.success('Routing policy saved', 'Provider routing policy was updated.');
    } catch (error) {
      const message = normalizeHttpError(error).message || 'Routing policy could not be saved.';
      this.errorSignal.set(message);
      this.notifications.error('Routing policy', message);
    } finally {
      this.savingSignal.set(false);
    }
  }
}
