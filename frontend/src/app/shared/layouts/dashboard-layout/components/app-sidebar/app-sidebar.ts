import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';

import { NavigationItem } from '@app/shared/layouts/navigation.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { BrandLogoComponent } from '@app/shared/components/brand-logo/brand-logo';
import { IconComponent } from '@app/shared/components/icon/icon';

interface NavigationGroup {
  readonly label: string;
  readonly items: readonly NavigationItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, BadgeComponent, BrandLogoComponent, IconComponent],
  templateUrl: './app-sidebar.html',
  styleUrl: './app-sidebar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly brandName = input('Lebhas - Brand Attire');
  readonly workspaceLabel = input('Select workspace');
  readonly role = input('ADMIN');
  readonly items = input<readonly NavigationItem[]>([]);
  readonly compact = input(false);
  readonly mobileOpen = input(false);
  readonly itemSelected = output<void>();
  readonly closeRequested = output<void>();

  readonly activeRoute = signal(this.router.url);

  protected readonly allowedNavItems = computed(() => this.items());
  protected readonly navigationGroups = computed(() => this.groupNavigationItems(this.allowedNavItems()));
  protected readonly activeNavigationItem = computed(
    () => this.allowedNavItems().find((item) => this.isActive(item)) ?? null,
  );
  protected readonly roleBadgeLabel = computed(() => this.role() || 'ADMIN');
  protected readonly displayWorkspaceName = computed(() => this.workspaceLabel() || 'Select workspace');
  protected readonly displayProjectName = computed(() => this.brandName() || 'Lebhas - Brand Attire');
  protected readonly compactRoleLabel = computed(() => this.roleBadgeLabel().slice(0, 1));

  constructor() {
    const subscription = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.activeRoute.set(event.urlAfterRedirects));

    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }

  protected isActive(item: NavigationItem): boolean {
    const currentRoute = this.normalizeRoute(this.activeRoute());
    const candidates = [item.route, ...(item.activePaths ?? [])].map((route) =>
      this.normalizeRoute(route),
    );

    return candidates.some((candidate) => {
      if (item.activeMatch === 'exact') {
        return currentRoute === candidate;
      }

      return currentRoute === candidate || currentRoute.startsWith(`${candidate}/`);
    });
  }

  protected selectItem(): void {
    this.itemSelected.emit();
  }

  protected roleBadgeTone(): 'brand' | 'blue' | 'red' | 'neutral' {
    const role = this.role();
    return role === 'MASTER' ? 'red' : role === 'CREW' ? 'blue' : 'brand';
  }

  private normalizeRoute(route: string): string {
    const routeWithoutQuery = route.split('?')[0]?.split('#')[0] ?? route;
    return routeWithoutQuery.length > 1 && routeWithoutQuery.endsWith('/')
      ? routeWithoutQuery.slice(0, -1)
      : routeWithoutQuery;
  }

  private groupNavigationItems(items: readonly NavigationItem[]): readonly NavigationGroup[] {
    const groups = new Map<string, NavigationItem[]>();

    for (const item of items) {
      const label = item.group ?? 'Workspace';
      groups.set(label, [...(groups.get(label) ?? []), item]);
    }

    return Array.from(groups.entries()).map(([label, groupItems]) => ({
      label,
      items: groupItems,
    }));
  }
}
