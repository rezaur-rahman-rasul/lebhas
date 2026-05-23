import { Page, Route } from '@playwright/test';

type UserRole = 'MASTER' | 'ADMIN' | 'CREW';

interface MockApiOptions {
  readonly role?: UserRole;
  readonly permissions?: readonly string[];
  readonly workspaceId?: string | null;
  readonly activeWorkspaceId?: string | null;
  readonly accessibleWorkspaceIds?: readonly string[];
  readonly responseDelayMs?: number;
}

interface SessionSeedOptions {
  readonly accessToken?: string;
  readonly refreshToken?: string;
  readonly accessTokenExpiresAt?: string;
  readonly refreshTokenExpiresAt?: string;
  readonly activeWorkspaceId?: string | null;
}

export interface MockApiHandle {
  readonly counters: {
    me: number;
    login: number;
    register: number;
    refresh: number;
    logout: number;
    brands: number;
    products: number;
    projects: number;
  };
  readonly ids: {
    primaryWorkspaceId: string;
    secondaryWorkspaceId: string;
    primaryBrandId: string;
    primaryProductId: string;
    primaryProjectId: string;
  };
}

interface BrandRecord {
  readonly id: string;
  readonly workspaceId: string;
  readonly ownerUserId: string;
  readonly name: string;
  readonly businessType: string | null;
  readonly industry: string | null;
  readonly targetAudience: string | null;
  readonly brandVoice: string | null;
  readonly preferredCta: string | null;
  readonly primaryColor: string | null;
  readonly secondaryColor: string | null;
  readonly website: string | null;
  readonly facebookUrl: string | null;
  readonly instagramUrl: string | null;
  readonly linkedinUrl: string | null;
  readonly tiktokUrl: string | null;
  readonly status: 'ACTIVE' | 'ARCHIVED';
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface ProductServiceRecord {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly name: string;
  readonly description: string | null;
  readonly category: string | null;
  readonly targetAudience: string | null;
  readonly sellingPoints: string | null;
  readonly status: 'ACTIVE' | 'ARCHIVED';
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface ProjectCampaignRecord {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId: string;
  readonly createdByUserId: string;
  readonly name: string;
  readonly description: string | null;
  readonly campaignObjective: string | null;
  readonly targetPlatform: string | null;
  readonly campaignType: string | null;
  readonly status: 'ACTIVE' | 'ARCHIVED';
  readonly createdAt: string;
  readonly updatedAt: string;
}

const PRIMARY_WORKSPACE_ID = '11111111-1111-1111-1111-111111111111';
const SECONDARY_WORKSPACE_ID = '22222222-2222-2222-2222-222222222222';
const PRIMARY_BRAND_ID = '33333333-3333-3333-3333-333333333333';
const SECONDARY_BRAND_ID = '44444444-4444-4444-4444-444444444444';
const PRIMARY_PRODUCT_ID = '55555555-5555-5555-5555-555555555555';
const SECONDARY_PRODUCT_ID = '66666666-6666-6666-6666-666666666666';
const PRIMARY_PROJECT_ID = '77777777-7777-7777-7777-777777777777';
const SECONDARY_PROJECT_ID = '88888888-8888-8888-8888-888888888888';

export async function seedStoredSession(
  page: Page,
  options: SessionSeedOptions = {},
): Promise<void> {
  const accessTokenExpiresAt =
    options.accessTokenExpiresAt ?? new Date(Date.now() + 15 * 60 * 1000).toISOString();
  const refreshTokenExpiresAt =
    options.refreshTokenExpiresAt ?? new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();

  await page.addInitScript((seed) => {
    window.localStorage.setItem('creative_saas.access_token', seed.accessToken);
    window.localStorage.setItem('creative_saas.refresh_token', seed.refreshToken);
    window.localStorage.setItem('creative_saas.access_token_expires_at', seed.accessTokenExpiresAt);
    window.localStorage.setItem('creative_saas.refresh_token_expires_at', seed.refreshTokenExpiresAt);

    if (seed.activeWorkspaceId) {
      window.localStorage.setItem('creative_saas.active_workspace_id', seed.activeWorkspaceId);
    } else {
      window.localStorage.removeItem('creative_saas.active_workspace_id');
    }
  }, {
    accessToken: options.accessToken ?? 'seeded-access-token',
    refreshToken: options.refreshToken ?? 'seeded-refresh-token',
    accessTokenExpiresAt,
    refreshTokenExpiresAt,
    activeWorkspaceId: options.activeWorkspaceId ?? null,
  });
}

export async function registerMockApi(
  page: Page,
  options: MockApiOptions = {},
): Promise<MockApiHandle> {
  const role = options.role ?? 'ADMIN';
  const workspaceId =
    options.workspaceId === undefined
      ? role === 'MASTER'
        ? null
        : PRIMARY_WORKSPACE_ID
      : options.workspaceId;
  const activeWorkspaceId =
    options.activeWorkspaceId === undefined
      ? role === 'MASTER'
        ? PRIMARY_WORKSPACE_ID
        : workspaceId ?? PRIMARY_WORKSPACE_ID
      : options.activeWorkspaceId;
  const accessibleWorkspaceIds =
    options.accessibleWorkspaceIds ??
    (role === 'MASTER'
      ? [PRIMARY_WORKSPACE_ID, SECONDARY_WORKSPACE_ID]
      : [workspaceId ?? PRIMARY_WORKSPACE_ID]);
  const permissions = [...(options.permissions ?? defaultPermissions(role))];
  const counters = {
    me: 0,
    login: 0,
    register: 0,
    refresh: 0,
    logout: 0,
    brands: 0,
    products: 0,
    projects: 0,
  };
  const user = buildUser(role, permissions, workspaceId);
  const workspaces = accessibleWorkspaceIds.map((id, index) =>
    buildWorkspace(id, index === 0 ? 'Lebhas HQ' : 'Lebhas Studio East', role, permissions),
  );

  let brands = buildBrands();
  let products = buildProducts();
  let projects = buildProjects();

  await page.route('**/api/v1/**', async (route) => {
    if ((options.responseDelayMs ?? 0) > 0) {
      await wait(options.responseDelayMs!);
    }

    const request = route.request();
    const url = new URL(request.url());
    const pathname = url.pathname;

    if (pathname === '/api/v1/auth/login' && request.method() === 'POST') {
      counters.login += 1;
      return json(route, 200, buildSession(role, permissions, workspaceId));
    }

    if (pathname === '/api/v1/auth/register' && request.method() === 'POST') {
      counters.register += 1;
      return json(route, 200, buildSession('ADMIN', defaultPermissions('ADMIN'), PRIMARY_WORKSPACE_ID));
    }

    if (pathname === '/api/v1/auth/refresh' && request.method() === 'POST') {
      counters.refresh += 1;
      return json(
        route,
        200,
        buildSession(role, permissions, workspaceId, {
          accessToken: `refreshed-access-token-${counters.refresh}`,
        }),
      );
    }

    if (pathname === '/api/v1/auth/logout' && request.method() === 'POST') {
      counters.logout += 1;
      return json(route, 200, null, 'Logout completed');
    }

    if (pathname === '/api/v1/auth/me' && request.method() === 'GET') {
      counters.me += 1;
      return json(route, 200, user);
    }

    if (pathname === '/api/v1/workspaces/me' && request.method() === 'GET') {
      return json(route, 200, workspaces);
    }

    const brandListMatch = pathname.match(/^\/api\/v1\/workspaces\/([^/]+)\/brands$/);
    if (brandListMatch && request.method() === 'GET') {
      counters.brands += 1;
      return json(
        route,
        200,
        brands.filter((brand) => brand.workspaceId === brandListMatch[1]),
      );
    }

    if (brandListMatch && request.method() === 'POST') {
      const payload = await request.postDataJSON();
      const nextBrand: BrandRecord = {
        id: cryptoId('brand'),
        workspaceId: brandListMatch[1],
        ownerUserId: user.id,
        name: payload.name,
        businessType: payload.businessType ?? null,
        industry: payload.industry ?? null,
        targetAudience: payload.targetAudience ?? null,
        brandVoice: payload.brandVoice ?? null,
        preferredCta: payload.preferredCta ?? null,
        primaryColor: payload.primaryColor ?? null,
        secondaryColor: payload.secondaryColor ?? null,
        website: payload.website ?? null,
        facebookUrl: payload.facebookUrl ?? null,
        instagramUrl: payload.instagramUrl ?? null,
        linkedinUrl: payload.linkedinUrl ?? null,
        tiktokUrl: payload.tiktokUrl ?? null,
        status: 'ACTIVE',
        createdAt: isoNow(),
        updatedAt: isoNow(),
      };
      brands = [nextBrand, ...brands];
      return json(route, 200, nextBrand);
    }

    const brandDetailMatch = pathname.match(/^\/api\/v1\/workspaces\/([^/]+)\/brands\/([^/]+)$/);
    if (brandDetailMatch && request.method() === 'GET') {
      const brand = brands.find((item) => item.id === brandDetailMatch[2]);
      return brand ? json(route, 200, brand) : error(route, 404, 'Brand not found');
    }

    if (brandDetailMatch && request.method() === 'PUT') {
      const payload = await request.postDataJSON();
      const current = brands.find((item) => item.id === brandDetailMatch[2]);
      if (!current) {
        return error(route, 404, 'Brand not found');
      }

      const updated: BrandRecord = {
        ...current,
        ...payload,
        updatedAt: isoNow(),
      };
      brands = brands.map((item) => (item.id === updated.id ? updated : item));
      return json(route, 200, updated);
    }

    if (brandDetailMatch && request.method() === 'DELETE') {
      brands = brands.filter((item) => item.id !== brandDetailMatch[2]);
      products = products.filter((item) => item.brandId !== brandDetailMatch[2]);
      projects = projects.filter((item) => item.brandId !== brandDetailMatch[2]);
      return json(route, 200, null, 'Brand deleted');
    }

    const productListMatch = pathname.match(/^\/api\/v1\/workspaces\/([^/]+)\/product-services$/);
    if (productListMatch && request.method() === 'GET') {
      counters.products += 1;
      return json(
        route,
        200,
        products.filter((product) => product.workspaceId === productListMatch[1]),
      );
    }

    const productCreateMatch = pathname.match(
      /^\/api\/v1\/workspaces\/([^/]+)\/brands\/([^/]+)\/product-services$/,
    );
    if (productCreateMatch && request.method() === 'POST') {
      const payload = await request.postDataJSON();
      const nextProduct: ProductServiceRecord = {
        id: cryptoId('product'),
        workspaceId: productCreateMatch[1],
        brandId: productCreateMatch[2],
        name: payload.name,
        description: payload.description ?? null,
        category: payload.category ?? null,
        targetAudience: payload.targetAudience ?? null,
        sellingPoints: payload.sellingPoints ?? null,
        status: 'ACTIVE',
        createdAt: isoNow(),
        updatedAt: isoNow(),
      };
      products = [nextProduct, ...products];
      return json(route, 200, nextProduct);
    }

    const productDetailMatch = pathname.match(
      /^\/api\/v1\/workspaces\/([^/]+)\/product-services\/([^/]+)$/,
    );
    if (productDetailMatch && request.method() === 'GET') {
      const product = products.find((item) => item.id === productDetailMatch[2]);
      return product ? json(route, 200, product) : error(route, 404, 'Product not found');
    }

    if (productDetailMatch && request.method() === 'PUT') {
      const payload = await request.postDataJSON();
      const current = products.find((item) => item.id === productDetailMatch[2]);
      if (!current) {
        return error(route, 404, 'Product not found');
      }

      const updated: ProductServiceRecord = {
        ...current,
        ...payload,
        updatedAt: isoNow(),
      };
      products = products.map((item) => (item.id === updated.id ? updated : item));
      return json(route, 200, updated);
    }

    if (productDetailMatch && request.method() === 'DELETE') {
      products = products.filter((item) => item.id !== productDetailMatch[2]);
      projects = projects.filter((item) => item.productServiceId !== productDetailMatch[2]);
      return json(route, 200, null, 'Product deleted');
    }

    const projectListMatch = pathname.match(/^\/api\/v1\/workspaces\/([^/]+)\/projects$/);
    if (projectListMatch && request.method() === 'GET') {
      counters.projects += 1;
      return json(
        route,
        200,
        projects.filter((project) => project.workspaceId === projectListMatch[1]),
      );
    }

    const projectCreateMatch = pathname.match(
      /^\/api\/v1\/workspaces\/([^/]+)\/product-services\/([^/]+)\/projects$/,
    );
    if (projectCreateMatch && request.method() === 'POST') {
      const payload = await request.postDataJSON();
      const parentProduct = products.find((item) => item.id === projectCreateMatch[2]);
      if (!parentProduct) {
        return error(route, 404, 'Product not found');
      }

      const nextProject: ProjectCampaignRecord = {
        id: cryptoId('project'),
        workspaceId: projectCreateMatch[1],
        brandId: parentProduct.brandId,
        productServiceId: parentProduct.id,
        createdByUserId: user.id,
        name: payload.name,
        description: payload.description ?? null,
        campaignObjective: payload.campaignObjective ?? null,
        targetPlatform: payload.targetPlatform ?? null,
        campaignType: payload.campaignType ?? null,
        status: 'ACTIVE',
        createdAt: isoNow(),
        updatedAt: isoNow(),
      };
      projects = [nextProject, ...projects];
      return json(route, 200, nextProject);
    }

    const projectDetailMatch = pathname.match(/^\/api\/v1\/workspaces\/([^/]+)\/projects\/([^/]+)$/);
    if (projectDetailMatch && request.method() === 'GET') {
      const project = projects.find((item) => item.id === projectDetailMatch[2]);
      return project ? json(route, 200, project) : error(route, 404, 'Project not found');
    }

    if (projectDetailMatch && request.method() === 'PUT') {
      const payload = await request.postDataJSON();
      const current = projects.find((item) => item.id === projectDetailMatch[2]);
      if (!current) {
        return error(route, 404, 'Project not found');
      }

      const updated: ProjectCampaignRecord = {
        ...current,
        ...payload,
        updatedAt: isoNow(),
      };
      projects = projects.map((item) => (item.id === updated.id ? updated : item));
      return json(route, 200, updated);
    }

    if (projectDetailMatch && request.method() === 'DELETE') {
      projects = projects.filter((item) => item.id !== projectDetailMatch[2]);
      return json(route, 200, null, 'Project deleted');
    }

    return error(route, 404, `No mock available for ${pathname}`);
  });

  return {
    counters,
    ids: {
      primaryWorkspaceId: activeWorkspaceId ?? PRIMARY_WORKSPACE_ID,
      secondaryWorkspaceId: SECONDARY_WORKSPACE_ID,
      primaryBrandId: PRIMARY_BRAND_ID,
      primaryProductId: PRIMARY_PRODUCT_ID,
      primaryProjectId: PRIMARY_PROJECT_ID,
    },
  };
}

function buildSession(
  role: UserRole,
  permissions: readonly string[],
  workspaceId: string | null,
  overrides?: { readonly accessToken?: string },
) {
  return {
    accessToken: overrides?.accessToken ?? 'mock-access-token',
    accessTokenExpiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
    refreshToken: 'mock-refresh-token',
    refreshTokenExpiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    user: buildUser(role, permissions, workspaceId),
  };
}

function buildUser(role: UserRole, permissions: readonly string[], workspaceId: string | null) {
  return {
    id: `user-${role.toLowerCase()}`,
    firstName: role === 'MASTER' ? 'Master' : role === 'ADMIN' ? 'Admin' : 'Crew',
    lastName: 'User',
    email: `${role.toLowerCase()}@example.com`,
    phone: null,
    role,
    status: 'ACTIVE',
    emailVerified: true,
    lastLoginAt: isoNow(),
    workspaceId,
    createdAt: isoNow(),
    updatedAt: isoNow(),
    permissions,
  };
}

function buildWorkspace(
  id: string,
  name: string,
  role: UserRole,
  permissions: readonly string[],
) {
  return {
    id,
    name,
    slug: name.toLowerCase().replace(/\s+/g, '-'),
    logoUrl: null,
    status: 'ACTIVE',
    language: 'ENGLISH',
    timezone: 'Asia/Dhaka',
    ownerId: 'owner-1',
    currentUserRole: role,
    currentUserPermissions: permissions,
    createdAt: isoNow(),
    updatedAt: isoNow(),
  };
}

function buildBrands(): BrandRecord[] {
  return [
    {
      id: PRIMARY_BRAND_ID,
      workspaceId: PRIMARY_WORKSPACE_ID,
      ownerUserId: 'user-admin',
      name: 'Lebhas Atelier',
      businessType: 'Apparel brand',
      industry: 'Fashion retail',
      targetAudience: 'Urban premium buyers',
      brandVoice: 'Modern, refined, confident',
      preferredCta: 'Shop the collection',
      primaryColor: '#0B8F68',
      secondaryColor: '#2563EB',
      website: 'https://atelier.example.com',
      facebookUrl: 'https://facebook.com/atelier',
      instagramUrl: 'https://instagram.com/atelier',
      linkedinUrl: 'https://linkedin.com/company/atelier',
      tiktokUrl: 'https://tiktok.com/@atelier',
      status: 'ACTIVE',
      createdAt: isoNow(),
      updatedAt: isoNow(),
    },
    {
      id: SECONDARY_BRAND_ID,
      workspaceId: PRIMARY_WORKSPACE_ID,
      ownerUserId: 'user-admin',
      name: 'Lebhas Studio',
      businessType: 'Packaging line',
      industry: 'Consumer goods',
      targetAudience: 'SMB retail buyers',
      brandVoice: 'Clear and commercial',
      preferredCta: 'Request a demo',
      primaryColor: '#1D4ED8',
      secondaryColor: '#F59E0B',
      website: 'https://studio.example.com',
      facebookUrl: null,
      instagramUrl: null,
      linkedinUrl: null,
      tiktokUrl: null,
      status: 'ACTIVE',
      createdAt: isoNow(),
      updatedAt: isoNow(),
    },
  ];
}

function buildProducts(): ProductServiceRecord[] {
  return [
    {
      id: PRIMARY_PRODUCT_ID,
      workspaceId: PRIMARY_WORKSPACE_ID,
      brandId: PRIMARY_BRAND_ID,
      name: 'Summer Capsule Collection',
      description: 'Primary seasonal apparel lineup for campaign production.',
      category: 'Apparel',
      targetAudience: 'Style-conscious professionals',
      sellingPoints: 'Breathable fabric, tailored silhouette, fast delivery',
      status: 'ACTIVE',
      createdAt: isoNow(),
      updatedAt: isoNow(),
    },
    {
      id: SECONDARY_PRODUCT_ID,
      workspaceId: PRIMARY_WORKSPACE_ID,
      brandId: SECONDARY_BRAND_ID,
      name: 'Packaging Mockup Bundle',
      description: 'Packaging-first catalog entry for retail poster campaigns.',
      category: 'Packaging',
      targetAudience: 'Retail operators',
      sellingPoints: 'Shelf-ready layout, premium finish, quick revision cycle',
      status: 'ACTIVE',
      createdAt: isoNow(),
      updatedAt: isoNow(),
    },
  ];
}

function buildProjects(): ProjectCampaignRecord[] {
  return [
    {
      id: PRIMARY_PROJECT_ID,
      workspaceId: PRIMARY_WORKSPACE_ID,
      brandId: PRIMARY_BRAND_ID,
      productServiceId: PRIMARY_PRODUCT_ID,
      createdByUserId: 'user-admin',
      name: 'Eid Launch Campaign',
      description: 'Seasonal launch campaign foundation for the summer capsule.',
      campaignObjective: 'Drive discovery and conversion',
      targetPlatform: 'Facebook + Instagram',
      campaignType: 'Launch campaign',
      status: 'ACTIVE',
      createdAt: isoNow(),
      updatedAt: isoNow(),
    },
    {
      id: SECONDARY_PROJECT_ID,
      workspaceId: PRIMARY_WORKSPACE_ID,
      brandId: SECONDARY_BRAND_ID,
      productServiceId: SECONDARY_PRODUCT_ID,
      createdByUserId: 'user-admin',
      name: 'Retail Packaging Push',
      description: 'Packaging concept rollout for LinkedIn and Facebook placements.',
      campaignObjective: 'Generate qualified B2B leads',
      targetPlatform: 'LinkedIn',
      campaignType: 'Lead campaign',
      status: 'ACTIVE',
      createdAt: isoNow(),
      updatedAt: isoNow(),
    },
  ];
}

function defaultPermissions(role: UserRole): readonly string[] {
  if (role === 'MASTER') {
    return [
      'USER_VIEW',
      'USER_CREATE',
      'USER_UPDATE',
      'USER_STATUS_UPDATE',
      'WORKSPACE_CREATE',
      'WORKSPACE_VIEW',
      'WORKSPACE_UPDATE',
      'WORKSPACE_STATUS_UPDATE',
      'WORKSPACE_SETTINGS_VIEW',
      'WORKSPACE_SETTINGS_UPDATE',
      'SESSION_MANAGE',
    ];
  }

  if (role === 'ADMIN') {
    return [
      'WORKSPACE_VIEW',
      'WORKSPACE_UPDATE',
      'BRAND_VIEW',
      'BRAND_MANAGE',
      'PRODUCT_VIEW',
      'PRODUCT_MANAGE',
      'PROJECT_VIEW',
      'PROJECT_CREATE',
      'PROJECT_UPDATE',
      'SESSION_MANAGE',
    ];
  }

  return ['WORKSPACE_VIEW', 'BRAND_VIEW', 'PRODUCT_VIEW', 'PROJECT_VIEW'];
}

function cryptoId(prefix: string): string {
  return `${prefix}-${Math.random().toString(16).slice(2, 10)}-${Date.now()}`;
}

function isoNow(): string {
  return new Date().toISOString();
}

function envelope<T>(data: T, message = 'OK') {
  return {
    success: true,
    message,
    data,
    errors: [],
    timestamp: isoNow(),
  };
}

async function json(route: Route, status: number, data: unknown, message = 'OK') {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(envelope(data, message)),
  });
}

async function error(route: Route, status: number, message: string) {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify({
      success: false,
      message,
      data: null,
      errors: [{ code: 'ERROR', message }],
      timestamp: isoNow(),
    }),
  });
}

async function wait(durationMs: number): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, durationMs));
}
