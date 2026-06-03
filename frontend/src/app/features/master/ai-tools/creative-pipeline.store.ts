import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  UiResourceState,
  uiResourceErrorKind,
  uiResourceErrorMessage,
} from '@app/shared/models/ui-resource-state.model';
import {
  ConfigureLayerRoutingPolicyPayload,
  CreativePipelineLayerView,
  CreativePipelineView,
  CreativeToolView,
  ProviderRoutingPolicyView,
} from './creative-pipeline.models';
import { CreativePipelineService } from './creative-pipeline.service';

@Injectable({ providedIn: 'root' })
export class CreativePipelineStore {
  private readonly service = inject(CreativePipelineService);
  private readonly notifications = inject(NotificationStateService);

  private readonly pipelinesSignal = signal<readonly CreativePipelineView[]>([]);
  private readonly toolsSignal = signal<readonly CreativeToolView[]>([]);
  private readonly standaloneLayersSignal = signal<readonly CreativePipelineLayerView[]>([]);
  private readonly routingPoliciesSignal = signal<readonly ProviderRoutingPolicyView[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly endpointUnavailableSignal = signal(false);
  private readonly stateSignal = signal<UiResourceState<readonly CreativeToolView[]>>({ status: 'idle' });

  readonly pipelines = this.pipelinesSignal.asReadonly();
  readonly tools = this.toolsSignal.asReadonly();
  readonly standaloneLayers = this.standaloneLayersSignal.asReadonly();
  readonly routingPolicies = this.routingPoliciesSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly endpointUnavailable = this.endpointUnavailableSignal.asReadonly();
  readonly state = this.stateSignal.asReadonly();
  readonly activePipelines = computed(() => this.pipelinesSignal().filter((pipeline) => pipeline.active));
  readonly layers = computed(() => this.pipelinesSignal().flatMap((pipeline) => pipeline.layers));

  async load(): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.endpointUnavailableSignal.set(false);
    this.stateSignal.set({ status: 'loading' });

    try {
      const [pipelines, tools, layers, routingPolicies] = await Promise.all([
        this.service.listPipelines().catch(() => []),
        this.service.listTools(),
        this.service.listLayers(),
        this.service.listRoutingPolicies(),
      ]);
      this.pipelinesSignal.set(pipelines);
      this.toolsSignal.set(tools);
      this.standaloneLayersSignal.set(layers);
      this.routingPoliciesSignal.set(routingPolicies);
      this.stateSignal.set(
        tools.length || layers.length || routingPolicies.length
          ? { status: 'success', data: tools }
          : { status: 'empty', message: 'No Master AI configuration records are available yet.' },
      );
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.endpointUnavailableSignal.set(normalized.status === 404);
      const message = this.resourceErrorMessage(normalized.status, normalized.message);
      this.errorSignal.set(message);
      this.stateSignal.set({
        status: 'error',
        message,
        code: uiResourceErrorKind(normalized.status),
        retryable: normalized.status === 0 || normalized.status >= 500,
      });
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

  private resourceErrorMessage(status: number, fallback?: string): string {
    const kind = uiResourceErrorKind(status);
    if (kind === 'endpoint-unavailable') {
      return 'The Master AI configuration API is not available yet.';
    }
    return uiResourceErrorMessage(kind, fallback);
  }
}
