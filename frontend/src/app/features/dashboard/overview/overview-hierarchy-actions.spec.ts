import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

describe('Admin dashboard hierarchy quick actions', () => {
  it('uses computed hierarchy counts and excludes unlinked projects from downstream actions', () => {
    const source = read('src/app/features/dashboard/overview/overview.ts');

    expect(source).toContain('protected readonly brandCount = computed');
    expect(source).toContain('protected readonly productServiceCount = computed');
    expect(source).toContain('protected readonly totalProjectCount = computed');
    expect(source).toContain('protected readonly validProjects = computed');
    expect(source).toContain("productServiceName.trim().toLowerCase() !== 'unknown product'");
    expect(source).toContain('productIds.has(project.productServiceId)');
    expect(source).toContain('protected readonly validProjectCount = computed');
    expect(source).toContain('this.productServiceCount() > 0 ? this.validProjects().length : 0');
    expect(source).toContain('protected readonly hasProjectLinkWarning = computed');
  });

  it('blocks quick actions with specific reasons and badges instead of generic blocked copy', () => {
    const source = read('src/app/features/dashboard/overview/overview.ts');
    const template = read('src/app/features/dashboard/overview/overview.html');

    expect(source).toContain("badgeLabel: canCreateBrand ? null : 'Permission required'");
    expect(source).toContain("badgeLabel: !canCreateProduct ? 'Permission required' : hasBrands ? null : 'Brand required'");
    expect(source).toContain("badgeLabel: !canCreateProject ? 'Permission required' : hasProducts ? null : 'Product required'");
    expect(source).toContain("disabledReason: this.downstreamDisabledReason(canUploadAsset, hasProducts, hasValidProject)");
    expect(source).toContain("disabledReason: this.downstreamDisabledReason(canCreatePrompt, hasProducts, hasValidProject)");
    expect(source).toContain('Create a product or service first.');
    expect(source).toContain('Create a linked project first.');
    expect(template).toContain('@if (!action.enabled)');
    expect(template).toContain('[attr.aria-disabled]="true"');
    expect(template).toContain('(click)="showDisabledActionToast(action)"');
    expect(template).toContain('{{ action.badgeLabel }}');
    expect(template).not.toContain('>Blocked<');
  });

  it('uses safe registered icons and visible icon containers for enabled and disabled cards', () => {
    const source = read('src/app/features/dashboard/overview/overview.ts');
    const template = read('src/app/features/dashboard/overview/overview.html');
    const styles = read('src/app/features/dashboard/overview/overview.scss');
    const icons = read('src/app/core/icons/platform-icons.provider.ts');

    expect(source).toContain('const QUICK_ACTION_ICONS');
    expect(source).toContain("CREATE_BRAND: 'shield-check'");
    expect(source).toContain("CREATE_PRODUCT_SERVICE: 'package-open'");
    expect(source).toContain("CREATE_PROJECT: 'folder-kanban'");
    expect(source).toContain("UPLOAD_ASSET: 'upload-cloud'");
    expect(source).toContain("CREATE_PROMPT: 'wand-sparkles'");
    expect(source).toContain("const QUICK_ACTION_FALLBACK_ICON = 'circle-help'");
    expect(source).toContain('getQuickActionIcon');
    expect(icons).toContain('CircleHelp');
    expect(template).toContain('class="quick-action-icon"');
    expect(template).toContain('class="quick-action-icon quick-action-icon--disabled"');
    expect(styles).toContain('.quick-action-icon');
    expect(styles).toContain('.quick-action-icon--disabled');
    expect(styles).toContain(':host-context(.dark) .quick-action-icon');
  });

  it('shows hierarchy warning when projects exist without a product link', () => {
    const template = read('src/app/features/dashboard/overview/overview.html');
    const source = read('src/app/features/dashboard/overview/overview.ts');

    expect(template).toContain('@if (hasProjectLinkWarning())');
    expect(template).toContain('Project needs product link');
    expect(template).toContain('One project exists, but it is not linked to a product or service.');
    expect(source).toContain("`${this.totalProjectCount()} active · product link missing`");
  });

  it('keeps modern Angular template control flow and avoids DOM hacks', () => {
    const template = read('src/app/features/dashboard/overview/overview.html');
    const source = read('src/app/features/dashboard/overview/overview.ts');

    expect(template).not.toMatch(/\*ngIf\b|\*ngFor\b/);
    expect(source).not.toMatch(/setTimeout|document\.querySelector|location\.reload|dispatchEvent/);
  });
});
