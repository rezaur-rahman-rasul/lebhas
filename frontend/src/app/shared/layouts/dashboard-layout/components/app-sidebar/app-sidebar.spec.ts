import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { BrandLogoComponent } from '@app/shared/components/brand-logo/brand-logo';
import { IconComponent } from '@app/shared/components/icon/icon';
import { DASHBOARD_NAVIGATION } from '../../dashboard-navigation';
import { SidebarComponent } from './app-sidebar';

@Component({
  selector: 'app-icon',
  standalone: true,
  template: '<span class="mock-icon" [attr.data-icon]="name()"></span>',
})
class MockIconComponent {
  readonly name = input.required<string>();
  readonly size = input(18);
}

@Component({
  selector: 'app-brand-logo',
  standalone: true,
  template: '<span class="mock-logo">Lebhas logo</span>',
})
class MockBrandLogoComponent {
  readonly size = input('sm');
  readonly elevated = input(false);
}

function paymentAppFiles(directory = join(process.cwd(), 'src/app')): string[] {
  const files: string[] = [];

  for (const entry of readdirSync(directory)) {
    const fullPath = join(directory, entry);
    const stats = statSync(fullPath);

    if (stats.isDirectory()) {
      files.push(...paymentAppFiles(fullPath));
      continue;
    }

    files.push(fullPath);
  }

  return files;
}

function relativeAppPath(filePath: string): string {
  return relative(join(process.cwd(), 'src/app'), filePath).replaceAll('\\', '/');
}

describe('SidebarComponent', () => {
  let fixture: ComponentFixture<SidebarComponent>;
  let router: Router;

  const adminItems = DASHBOARD_NAVIGATION.filter((item) => item.roles?.includes('ADMIN')).filter(
    (item) => !item.permissionKey || ['canViewBrands', 'canViewProducts', 'canViewProjects'].includes(item.permissionKey),
  );

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [
        provideRouter([
          { path: 'dashboard', component: SidebarComponent },
          { path: 'brands/:id/edit', component: SidebarComponent },
          { path: 'projects/:id/assets', component: SidebarComponent },
        ]),
      ],
    })
      .overrideComponent(SidebarComponent, {
        remove: { imports: [IconComponent, BrandLogoComponent] },
        add: { imports: [MockIconComponent, MockBrandLogoComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(SidebarComponent);
    router = TestBed.inject(Router);
    fixture.componentRef.setInput('brandName', 'Lebhas - Brand Attire');
    fixture.componentRef.setInput(
      'workspaceLabel',
      'A very long workspace name that should truncate inside the sidebar instead of overflowing',
    );
    fixture.componentRef.setInput('role', 'ADMIN');
    fixture.componentRef.setInput('items', adminItems);
    fixture.detectChanges();
  });

  it('renders logo, project name, and long workspace text with truncation classes', () => {
    const host: HTMLElement = fixture.nativeElement;

    expect(host.textContent).toContain('Lebhas - Brand Attire');
    expect(host.textContent).toContain('A very long workspace name');
    expect(host.querySelector('.sidebar-brand-title')?.className).toContain('sidebar-brand-title');
    expect(host.querySelector('.sidebar-brand-subtitle')?.className).toContain('sidebar-brand-subtitle');
  });

  it('renders core ADMIN navigation items consistently', () => {
    const host: HTMLElement = fixture.nativeElement;

    expect(host.querySelector('[data-testid="sidebar-link-dashboard"]')).not.toBeNull();
    expect(host.querySelector('[data-testid="sidebar-link-brands"]')).not.toBeNull();
    expect(host.querySelector('[data-testid="sidebar-link-products-services"]')).not.toBeNull();
    expect(host.querySelector('[data-testid="sidebar-link-projects-campaigns"]')).not.toBeNull();
  });

  it('keeps restricted MASTER navigation out of the ADMIN item set', () => {
    const labels = adminItems.map((item) => item.label);

    expect(labels).not.toContain('Payment Providers');
    expect(labels).not.toContain('Provider Settings');
  });

  it('keeps CREW restricted product items hidden by role config', () => {
    const crewLabels = DASHBOARD_NAVIGATION.filter((item) => item.roles?.includes('CREW')).map(
      (item) => item.label,
    );

    expect(crewLabels).not.toContain('Products/Services');
    expect(crewLabels).not.toContain('Brands');
  });

  it('highlights active parent navigation for child routes', async () => {
    await router.navigateByUrl('/brands/123/edit');
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="sidebar-link-brands"]')?.className,
    ).toContain('sidebar-link--active');
  });

  it('keeps Projects/Campaigns active for project child routes', async () => {
    await router.navigateByUrl('/projects/123/assets');
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="sidebar-link-projects-campaigns"]')
        ?.className,
    ).toContain('sidebar-link--active');
  });

  it('collapses labels while keeping icon links accessible', () => {
    fixture.componentRef.setInput('compact', true);
    fixture.detectChanges();

    const host: HTMLElement = fixture.nativeElement;
    expect(host.textContent).not.toContain('Products/Services');
    expect(host.querySelector('[data-testid="sidebar-link-products-services"]')).not.toBeNull();
    expect(host.querySelector('[data-testid="sidebar-link-products-services"]')?.getAttribute('aria-label')).toBe(
      'Products/Services',
    );
  });

  it('opens and closes in mobile mode and closes after a nav click', () => {
    let selected = 0;
    let closed = 0;
    fixture.componentRef.setInput('mobileOpen', true);
    fixture.componentInstance.itemSelected.subscribe(() => selected += 1);
    fixture.componentInstance.closeRequested.subscribe(() => closed += 1);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-testid="sidebar-link-dashboard"]')?.click();
    fixture.nativeElement.querySelector('.sidebar-close')?.click();

    expect(selected).toBe(1);
    expect(closed).toBe(1);
    expect(fixture.nativeElement.querySelector('.sidebar-shell')?.className).toContain(
      'sidebar-shell--mobile-open',
    );
  });

  it('renders the access scope card with role badge content', () => {
    const host: HTMLElement = fixture.nativeElement;

    expect(host.textContent).toContain('ACCESS SCOPE');
    expect(host.textContent).toContain('ADMIN');
    expect(host.querySelector('.access-scope-card')).not.toBeNull();
  });

  it('contains dark and light theme compatibility styles', () => {
    const styles = readFileSync(
      join(
        process.cwd(),
        'src/app/shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.scss',
      ),
      'utf8',
    );

    expect(styles).toContain('.sidebar-shell');
    expect(styles).toContain(":host-context(.light)");
    expect(styles).toContain("[data-theme='light']");
    expect(styles).toContain('overflow: hidden');
  });

  it('uses one sidebar component and no old structural directive syntax', () => {
    const appFiles = paymentAppFiles();
    const sidebarComponents = appFiles
      .map(relativeAppPath)
      .filter((file) => file.endsWith('app-sidebar.ts') || file.endsWith('sidebar.ts'));
    const template = readFileSync(
      join(
        process.cwd(),
        'src/app/shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.html',
      ),
      'utf8',
    );

    expect(sidebarComponents).toEqual([
      'shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.ts',
    ]);
    expect(template).not.toContain('*ngIf');
    expect(template).not.toContain('*ngFor');
  });
});
