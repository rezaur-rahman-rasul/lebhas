import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

describe('Final Admin UI consistency cleanup', () => {
  it('keeps Admin navigation grouped and payment access permission-aware', () => {
    const navigation = read('src/app/shared/layouts/dashboard-layout/dashboard-navigation.ts');

    expect(navigation).toContain("label: 'Dashboard'");
    expect(navigation).toContain("group: 'Workspace'");
    expect(navigation).toContain("label: 'Payments'");
    expect(navigation).toContain("permissionKey: 'canViewPayments'");
    expect(navigation).toContain("label: 'Approvals'");
    expect(navigation).toContain("group: 'Overview'");
  });

  it('keeps the Admin dashboard header and hierarchy-aware quick actions', () => {
    const template = read('src/app/features/dashboard/overview/overview.html');
    const source = read('src/app/features/dashboard/overview/overview.ts');

    expect(template).toContain('<app-section-header eyebrow="Workspace overview" title="Dashboard"');
    expect(template).toContain('@if (!action.enabled)');
    expect(template).toContain('quick-action-card--disabled');
    expect(source).toContain('Create a brand first.');
    expect(source).toContain('Create a product or service first.');
    expect(source).toContain('Create a linked project first.');
    expect(source).toContain('void this.brands.load(workspaceId)');
    expect(source).toContain('void this.products.load(workspaceId)');
    expect(source).toContain('void this.projects.load(workspaceId)');
  });

  it('uses the final Products and Projects dependency wording and disabled create states', () => {
    const products = read('src/app/features/product-services/product-services.html');
    const productSource = read('src/app/features/product-services/product-services.ts');
    const projects = read('src/app/features/projects/projects.html');
    const projectSource = read('src/app/features/projects/projects.ts');

    expect(products).toContain('title="No products or services yet"');
    expect(products).toContain('description="Create a product or service once the brand layer is ready."');
    expect(products).toContain('[attr.title]="brands().length === 0 ? \'Create a brand first.\' : null"');
    expect(products).toContain('[disabled]="brands().length === 0"');
    expect(productSource).toContain('if (this.brands().length === 0)');
    expect(projects).toContain('[attr.title]="products().length === 0 ? \'Create a product or service first.\' : null"');
    expect(projects).toContain('Product not linked');
    expect(projects).toContain('Link this project to a product/service before creative generation.');
    expect(projectSource).toContain('isProductLinked');
    expect(projectSource).toContain("'Product not linked'");
  });

  it('uses the shared sticky modal shell and consistent Admin create form footer pattern', () => {
    const dialogTemplate = read('src/app/shared/components/app-dialog/app-dialog.html');
    const styles = read('src/styles.scss');
    const brands = read('src/app/features/brands/brands.html');
    const products = read('src/app/features/product-services/product-services.html');
    const projects = read('src/app/features/projects/projects.html');

    expect(dialogTemplate).toContain('sticky top-0');
    expect(styles).toContain('.admin-modal-form');
    expect(styles).toContain('.admin-modal-footer');
    expect(styles).toContain('position: sticky');
    expect(brands).toContain('Basic Brand Info');
    expect(brands).toContain('Brand Voice & CTA');
    expect(brands).toContain('Visual Identity');
    expect(brands).toContain('Online Presence');
    expect(brands).toContain('class="admin-modal-footer"');
    expect(products).toContain('class="admin-modal-footer"');
    expect(projects).toContain('class="admin-modal-footer"');
  });

  it('keeps page-level approval errors friendly without duplicate load toasts', () => {
    const approvals = read('src/app/features/approvals/pages/approval-queue/approval-queue.html');
    const store = read('src/app/features/approvals/approval.store.ts');

    expect(approvals).toContain('title="Approvals could not be loaded"');
    expect(approvals).toContain('description="We could not load this data. Please try again."');
    expect(store).not.toContain("this.notifications.error('Approvals'");
    expect(store).toContain("this.notifications.error('Approval failed'");
  });

  it('preserves the avatar-only topbar trigger and avoids legacy Angular syntax in touched Admin templates', () => {
    const profile = read('src/app/core/layout/user-profile-dropdown/user-profile-dropdown.html');
    const touchedTemplates = [
      'src/app/features/dashboard/overview/overview.html',
      'src/app/features/brands/brands.html',
      'src/app/features/product-services/product-services.html',
      'src/app/features/projects/projects.html',
      'src/app/features/approvals/pages/approval-queue/approval-queue.html',
    ].map(read).join('\n');

    expect(profile).toContain('aria-label="Open user menu"');
    expect(profile).toContain('<app-avatar [name]="displayName()" [avatarUrl]="avatarUrl()" size="md" />');
    expect(touchedTemplates).not.toMatch(/\*ngIf\b|\*ngFor\b/);
  });
});
