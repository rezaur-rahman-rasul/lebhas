import { signal } from '@angular/core';

import { Asset } from '@app/features/assets/models/asset.models';
import { Brand } from '@app/features/brands/brand.models';
import { ProductServiceRecord } from '@app/features/product-services/product-service.models';
import { ProjectCampaign } from '@app/features/projects/project.models';
import { PromptHistory, PromptTemplate } from '../models';
import { DEFAULT_PROMPT_BUILDER_SETTINGS, SelectedProjectContext } from '../state/prompt.state';

export const mockWorkspaceId = 'workspace-1';
export const mockProjectId = 'project-1';
export const mockBrandId = 'brand-1';
export const mockProductId = 'product-1';

export const mockBrand: Brand = {
  id: mockBrandId,
  workspaceId: mockWorkspaceId,
  ownerUserId: 'user-1',
  name: 'Lebhas Studio',
  businessType: 'Fashion retail',
  industry: 'Apparel',
  targetAudience: 'Young professionals',
  languagePreference: 'BOTH',
  brandVoice: 'Confident and modern',
  preferredCta: 'Shop now',
  primaryColor: '#111827',
  secondaryColor: '#f59e0b',
  website: null,
  facebookUrl: null,
  instagramUrl: null,
  linkedinUrl: null,
  tiktokUrl: null,
  status: 'ACTIVE',
  createdAt: '2026-05-17T00:00:00.000Z',
  updatedAt: '2026-05-17T00:00:00.000Z',
};

export const mockProduct: ProductServiceRecord = {
  id: mockProductId,
  workspaceId: mockWorkspaceId,
  brandId: mockBrandId,
  name: 'Summer Collection',
  description: 'Seasonal apparel line',
  category: 'Apparel',
  targetAudience: 'Style-conscious shoppers',
  sellingPoints: 'Premium fabrics, limited drops',
  status: 'ACTIVE',
  createdAt: '2026-05-17T00:00:00.000Z',
  updatedAt: '2026-05-17T00:00:00.000Z',
};

export const mockProject: ProjectCampaign = {
  id: mockProjectId,
  workspaceId: mockWorkspaceId,
  brandId: mockBrandId,
  productServiceId: mockProductId,
  createdByUserId: 'user-1',
  name: 'Summer Launch',
  description: 'Launch campaign for summer collection',
  campaignObjective: 'AWARENESS',
  targetPlatform: 'INSTAGRAM',
  campaignType: 'Product launch',
  status: 'ACTIVE',
  createdAt: '2026-05-17T00:00:00.000Z',
  updatedAt: '2026-05-17T00:00:00.000Z',
};

export const mockProjectContext: SelectedProjectContext = {
  workspaceId: mockWorkspaceId,
  projectId: mockProjectId,
  brandId: mockBrandId,
  productServiceId: mockProductId,
  brandName: mockBrand.name,
  productName: mockProduct.name,
  projectName: mockProject.name,
};

export const mockProjectAsset: Asset = {
  id: 'asset-project-1',
  workspaceId: mockWorkspaceId,
  brandId: mockBrandId,
  productServiceId: mockProductId,
  projectCampaignId: mockProjectId,
  storageFileId: 'file-1',
  uploadedBy: 'user-1',
  assetType: 'PRODUCT_IMAGE',
  assetCategory: 'PRODUCT_IMAGE',
  originalFileName: 'hero.jpg',
  displayName: 'Hero product shot',
  description: null,
  tags: [],
  uploadSessionId: null,
  previewStatus: 'READY',
  processingStatus: 'READY',
  status: 'READY',
  createdAt: '2026-05-17T00:00:00.000Z',
  updatedAt: '2026-05-17T00:00:00.000Z',
};

export const mockOtherProjectAsset: Asset = {
  ...mockProjectAsset,
  id: 'asset-project-2',
  projectCampaignId: 'project-2',
  displayName: 'Other project asset',
};

export const mockTemplate: PromptTemplate = {
  id: 'template-1',
  workspaceId: mockWorkspaceId,
  name: 'Instagram awareness',
  category: 'Launch',
  description: 'Awareness template',
  platform: 'INSTAGRAM',
  campaignObjective: 'AWARENESS',
  businessType: 'Fashion retail',
  language: 'ENGLISH',
  promptBody: 'Write a concise Instagram awareness prompt for a fashion launch.',
  isDefault: false,
  status: 'ACTIVE',
  createdBy: 'user-1',
  createdAt: '2026-05-17T00:00:00.000Z',
  updatedAt: '2026-05-17T00:00:00.000Z',
};

export const mockHistoryEntry: PromptHistory = {
  id: 'history-1',
  workspaceId: mockWorkspaceId,
  projectCampaignId: mockProjectId,
  creativeRequestId: null,
  sourcePrompt: 'Promote the summer collection with a confident tone.',
  enhancedPrompt: 'Launch the summer collection with confident, modern styling cues.',
  language: 'ENGLISH',
  platform: 'INSTAGRAM',
  campaignObjective: 'AWARENESS',
  businessType: 'Fashion retail',
  brandContextSnapshot: { brandName: 'Lebhas Studio', industry: 'Apparel' },
  suggestionType: 'ENHANCEMENT',
  status: 'SUCCEEDED',
  aiProvider: null,
  model: null,
  tokenUsage: 128,
  createdBy: 'user-1',
  createdAt: '2026-05-17T01:00:00.000Z',
};

export const mockBuilderSettings = {
  ...DEFAULT_PROMPT_BUILDER_SETTINGS,
  platform: 'INSTAGRAM' as const,
  campaignObjective: 'AWARENESS' as const,
  language: 'ENGLISH' as const,
  tone: 'PROFESSIONAL' as const,
};

export function createAuthMock(options?: {
  readonly workspaceId?: string | null;
  readonly role?: 'ADMIN' | 'CREW' | 'MASTER';
  readonly permissions?: readonly string[];
}) {
  const workspaceId = options?.workspaceId === undefined ? mockWorkspaceId : options.workspaceId;
  const role = options?.role ?? 'ADMIN';
  const permissions = options?.permissions ?? ['PROMPT_INTELLIGENCE_USE', 'PROMPT_HISTORY_VIEW', 'PROMPT_TEMPLATE_MANAGE'];

  return {
    activeWorkspaceId: vi.fn(() => workspaceId),
    currentRole: vi.fn(() => role),
    permissions: vi.fn(() => permissions),
    hasPermission: vi.fn((permission: string) => permissions.includes(permission)),
    currentUser: vi.fn(() => ({ id: 'user-1', workspaceName: 'Lebhas Workspace' })),
  };
}

export function createItemsSignal<T>(items: readonly T[]) {
  return signal(items).asReadonly();
}
