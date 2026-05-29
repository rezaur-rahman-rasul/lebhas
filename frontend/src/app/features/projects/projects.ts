import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  afterNextRender,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { InputComponent } from '@app/shared/components/input/input';
import { ModalComponent } from '@app/shared/components/modal/modal';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import { BrandStore } from '../brands/brand.store';
import { ProductServiceStore } from '../product-services/product-service.store';
import {
  CreateProjectCampaignPayload,
  ProjectCampaign,
  ProjectCampaignStatus,
} from './project.models';
import { ProjectStore } from './project.store';

type ProjectDialogMode = 'create' | 'edit' | null;
const CAMPAIGN_OBJECTIVES: readonly string[] = [
  'Product awareness',
  'Sales conversion',
  'New collection launch',
  'Retargeting',
];
const TARGET_PLATFORMS: readonly string[] = [
  'Multi-platform',
  'Facebook + Instagram',
  'TikTok',
  'LinkedIn',
];

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    InputComponent,
    ModalComponent,
    PageHeaderComponent,
  ],
  templateUrl: './projects.html',
  styleUrl: './projects.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectsComponent implements AfterViewInit {
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly workspace = inject(WorkspaceStore);
  private readonly permissions = inject(PermissionStore);
  protected readonly brandStore = inject(BrandStore);
  protected readonly productStore = inject(ProductServiceStore);
  protected readonly store = inject(ProjectStore);

  protected readonly selectedProjectId = signal<string | null>(
    this.route.snapshot.queryParamMap.get('projectId'),
  );
  protected readonly dialogMode = signal<ProjectDialogMode>(null);
  protected readonly attemptedSubmit = signal(false);
  protected readonly formProductId = signal<string | null>(null);

  protected readonly canView = this.permissions.canViewProjects;
  protected readonly canCreate = this.permissions.canCreateProjects;
  protected readonly canUpdate = this.permissions.canUpdateProjects;
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly workspaceLabel = this.workspace.workspaceLabel;
  protected readonly brands = this.brandStore.items;
  protected readonly products = this.productStore.items;
  protected readonly projects = this.store.items;
  protected readonly campaignObjectives = CAMPAIGN_OBJECTIVES;
  protected readonly targetPlatforms = TARGET_PLATFORMS;
  protected readonly skeletonRows = [1, 2, 3, 4] as const;
  protected readonly hasProductServices = computed(() => this.products().length > 0);
  protected readonly selectedProject = computed(
    () => this.projects().find((project) => project.id === this.selectedProjectId()) ?? null,
  );
  protected readonly selectedProduct = computed(() => {
    const project = this.selectedProject();
    return this.products().find((product) => product.id === project?.productServiceId) ?? null;
  });
  protected readonly selectedBrand = computed(() => {
    const project = this.selectedProject();
    return this.brands().find((brand) => brand.id === project?.brandId) ?? null;
  });
  protected readonly formProduct = computed(() => {
    const productId = this.formProductId();
    return this.products().find((product) => product.id === productId) ?? null;
  });
  protected readonly formBrand = computed(() => {
    const product = this.formProduct();
    return this.brands().find((brand) => brand.id === product?.brandId) ?? null;
  });

  protected readonly form = this.formBuilder.group({
    productServiceId: ['', [Validators.required]],
    name: ['', [Validators.required]],
    description: [''],
    campaignObjective: [''],
    targetPlatform: [''],
    campaignType: [''],
    status: ['ACTIVE' as ProjectCampaignStatus],
  });

  constructor() {
    afterNextRender(() => this.resetRouteViewport());

    effect(() => {
      const workspaceId = this.workspaceId();
      if (!workspaceId) {
        return;
      }

      if (this.permissions.canViewBrands()) {
        void this.brandStore.load(workspaceId);
      }

      if (this.permissions.canViewProducts()) {
        void this.productStore.load(workspaceId);
      }

      if (this.canView()) {
        void this.store.load(workspaceId);
      }
    });

    effect(() => {
      const projects = this.projects();
      const currentSelection = this.selectedProjectId();

      if (projects.length === 0) {
        this.selectedProjectId.set(null);
        return;
      }

      if (!currentSelection || !projects.some((project) => project.id === currentSelection)) {
        this.selectedProjectId.set(projects[0].id);
      }
    });
  }

  ngAfterViewInit(): void {
    this.queueViewportReset();
  }

  protected selectProject(projectId: string): void {
    this.selectedProjectId.set(projectId);
  }

  protected openCreateDialog(): void {
    if (this.products().length === 0) {
      return;
    }

    const defaultProductId = this.products()[0]?.id ?? '';
    this.attemptedSubmit.set(false);
    this.form.reset({
      productServiceId: defaultProductId,
      name: '',
      description: '',
      campaignObjective: 'Product awareness',
      targetPlatform: 'Multi-platform',
      campaignType: 'Launch campaign',
      status: 'ACTIVE',
    });
    this.formProductId.set(defaultProductId || null);
    this.dialogMode.set('create');
  }

  protected openEditDialog(): void {
    const project = this.selectedProject();
    if (!project) {
      return;
    }

    this.attemptedSubmit.set(false);
    this.form.reset({
      productServiceId: project.productServiceId,
      name: project.name,
      description: project.description ?? '',
      campaignObjective: project.campaignObjective ?? '',
      targetPlatform: project.targetPlatform ?? '',
      campaignType: project.campaignType ?? '',
      status: project.status,
    });
    this.formProductId.set(project.productServiceId);
    this.dialogMode.set('edit');
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.formProductId.set(null);
  }

  protected updateFormProduct(event: Event): void {
    this.formProductId.set((event.target as HTMLSelectElement).value || null);
  }

  protected async submit(): Promise<void> {
    const workspaceId = this.workspaceId();
    if (!workspaceId) {
      return;
    }

    this.attemptedSubmit.set(true);
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    const payload = this.toPayload();

    try {
      if (this.dialogMode() === 'create') {
        const project = await this.store.create(workspaceId, value.productServiceId, payload);
        this.selectedProjectId.set(project.id);
      } else {
        const project = this.selectedProject();
        if (!project) {
          return;
        }

        const updatedProject = await this.store.update(workspaceId, project.id, {
          ...payload,
          status: value.status,
        });
        this.selectedProjectId.set(updatedProject.id);
      }
    } catch {
      return;
    }

    this.closeDialog();
  }

  protected async deleteSelected(): Promise<void> {
    const workspaceId = this.workspaceId();
    const project = this.selectedProject();
    if (!workspaceId || !project) {
      return;
    }

    const confirmed = globalThis.confirm(`Delete ${project.name}?`);
    if (!confirmed) {
      return;
    }

    await this.store.remove(workspaceId, project.id);
  }

  protected productName(productServiceId: string): string {
    return this.products().find((product) => product.id === productServiceId)?.name ?? 'Product not linked';
  }

  protected isProductLinked(productServiceId: string): boolean {
    return this.products().some((product) => product.id === productServiceId);
  }

  protected brandName(brandId: string): string {
    return this.brands().find((brand) => brand.id === brandId)?.name ?? 'Unknown brand';
  }

  protected projectDescription(project: ProjectCampaign): string {
    return project.description || `Primary project for ${this.workspaceLabel()}`;
  }

  protected retryProjects(): void {
    const workspaceId = this.workspaceId();
    if (workspaceId) {
      void this.store.load(workspaceId, { force: true });
    }
  }

  protected fieldError(fieldName: 'productServiceId' | 'name'): string {
    const control = this.form.controls[fieldName];

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return fieldName === 'productServiceId'
        ? 'Select a product or service.'
        : 'Enter a project or campaign name.';
    }

    return '';
  }

  private toPayload(): CreateProjectCampaignPayload {
    const value = this.form.getRawValue();

    return {
      name: value.name.trim(),
      description: this.normalize(value.description),
      campaignObjective: this.normalize(value.campaignObjective),
      targetPlatform: this.normalize(value.targetPlatform),
      campaignType: this.normalize(value.campaignType),
    };
  }

  private normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private queueViewportReset(): void {
    if (typeof window === 'undefined') {
      return;
    }

    const frameId = window.requestAnimationFrame(() => this.resetRouteViewport());
    const timeoutIds = [0, 75].map((delay) =>
      window.setTimeout(() => this.resetRouteViewport(), delay),
    );

    this.destroyRef.onDestroy(() => {
      window.cancelAnimationFrame(frameId);
      timeoutIds.forEach((timeoutId) => window.clearTimeout(timeoutId));
    });
  }

  private resetRouteViewport(): void {
    if (typeof window === 'undefined') {
      return;
    }

    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
    document.documentElement.scrollTop = 0;
    document.documentElement.scrollLeft = 0;
    document.body.scrollTop = 0;
    document.body.scrollLeft = 0;

    document.querySelector('main')?.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }
}
