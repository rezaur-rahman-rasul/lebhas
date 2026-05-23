import { Injectable, computed, inject, signal } from '@angular/core';

import {
  CreateProjectCampaignPayload,
  ProjectCampaign,
  UpdateProjectCampaignPayload,
} from './project.models';
import { ProjectCampaignService } from './project.service';

@Injectable({ providedIn: 'root' })
export class ProjectStore {
  private readonly service = inject(ProjectCampaignService);

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

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const projects = await this.service.list(workspaceId);
      this.itemsSignal.set(projects);
      this.loadedWorkspaceIdSignal.set(workspaceId);
    } catch {
      this.itemsSignal.set([]);
      this.errorSignal.set('Projects could not be loaded.');
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
      return project;
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
      return project;
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
    } finally {
      this.savingSignal.set(false);
    }
  }
}
