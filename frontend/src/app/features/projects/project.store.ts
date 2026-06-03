import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  CreateProjectCampaignPayload,
  ProjectCampaign,
  UpdateProjectCampaignPayload,
} from './project.models';
import { ProjectApiService } from './project-api.service';

@Injectable({ providedIn: 'root' })
export class ProjectStore {
  private readonly service = inject(ProjectApiService);
  private readonly notifications = inject(NotificationStateService);

  private readonly itemsSignal = signal<readonly ProjectCampaign[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly loadedWorkspaceIdSignal = signal<string | null>(null);

  readonly items = this.itemsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly total = computed(() => this.itemsSignal().length);

  async load(workspaceId: string, options?: { readonly force?: boolean }): Promise<void> {
    if (
      !options?.force &&
      this.loadedWorkspaceIdSignal() === workspaceId &&
      (this.itemsSignal().length > 0 || this.errorSignal() === null)
    ) {
      return;
    }

    if (this.loadedWorkspaceIdSignal() !== workspaceId) {
      this.itemsSignal.set([]);
      this.loadedWorkspaceIdSignal.set(workspaceId);
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const projects = await this.service.list(workspaceId);
      this.itemsSignal.set(projects);
      this.loadedWorkspaceIdSignal.set(workspaceId);
    } catch (error) {
      this.itemsSignal.set([]);
      this.errorSignal.set(normalizeHttpError(error).message || 'Projects could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async create(
    workspaceId: string,
    productServiceId: string,
    payload: CreateProjectCampaignPayload,
  ): Promise<ProjectCampaign> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const project = await this.service.create(workspaceId, productServiceId, payload);
      this.itemsSignal.update((items) => [project, ...items]);
      this.loadedWorkspaceIdSignal.set(workspaceId);
      this.notifications.success('Project created', `${project.name} is ready for assets and prompts.`);
      return project;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async update(
    workspaceId: string,
    projectId: string,
    payload: UpdateProjectCampaignPayload,
  ): Promise<ProjectCampaign> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const project = await this.service.update(workspaceId, projectId, payload);
      this.itemsSignal.update((items) => items.map((item) => (item.id === project.id ? project : item)));
      this.notifications.success('Project updated', `${project.name} changes were saved.`);
      return project;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async remove(workspaceId: string, projectId: string): Promise<void> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await this.service.remove(workspaceId, projectId);
      this.itemsSignal.update((items) => items.filter((item) => item.id !== projectId));
      this.notifications.success('Project deleted', 'The project campaign was removed.');
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
    } finally {
      this.savingSignal.set(false);
    }
  }

  reset(): void {
    this.itemsSignal.set([]);
    this.loadedWorkspaceIdSignal.set(null);
    this.errorSignal.set(null);
    this.loadingSignal.set(false);
    this.savingSignal.set(false);
  }
}
