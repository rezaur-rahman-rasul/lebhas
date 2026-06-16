import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  RouterLinkActive,
  NavigationSkipped,
  NavigationStart,
  Router,
  RouterLink,
} from '@angular/router';
import { filter, map, startWith } from 'rxjs';

import { NavigationItem } from '@app/shared/layouts/navigation.models';
import { IconComponent } from '@app/shared/components/icon/icon';
import { BillingModalService } from '@app/features/payments/services/billing-modal.service';
import {NgOptimizedImage} from '@angular/common';

interface NavigationGroup {
  readonly label: string;
  readonly items: readonly NavigationItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, IconComponent, NgOptimizedImage],
  templateUrl: './app-sidebar.html',
  styleUrl: './app-sidebar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  private readonly router = inject(Router);
  private readonly billingModal = inject(BillingModalService);

  readonly brandName = input('Lebhas - Brand Attire');
  readonly workspaceLabel = input('Select workspace');
  readonly role = input('ADMIN');
  readonly items = input<readonly NavigationItem[]>([]);
  readonly compact = input(false);
  readonly mobileOpen = input(false);
  readonly itemSelected = output<void>();
  readonly closeRequested = output<void>();

  readonly activeRoute = toSignal(
    this.router.events.pipe(
      filter(
        (
          event,
        ): event is
          | NavigationStart
          | NavigationEnd
          | NavigationCancel
          | NavigationError
          | NavigationSkipped =>
          event instanceof NavigationStart ||
          event instanceof NavigationEnd ||
          event instanceof NavigationCancel ||
          event instanceof NavigationError ||
          event instanceof NavigationSkipped,
      ),
      map((event) => this.normalizeRoute(this.routeFromNavigationEvent(event))),
      startWith(this.normalizeRoute(this.router.url)),
    ),
    { initialValue: this.normalizeRoute(this.router.url) },
  );

  protected readonly allowedNavItems = computed(() => this.items());
  protected readonly navigationGroups = computed(() => this.groupNavigationItems(this.allowedNavItems()));
  protected readonly activeNavigationItem = computed(
    () => {
      this.activeRoute();
      return this.allowedNavItems().find((item) => this.isActive(item)) ?? null;
    },
  );
  protected readonly roleBadgeLabel = computed(() => this.role() || 'ADMIN');
  protected readonly displayWorkspaceName = computed(() => this.workspaceLabel() || 'Select workspace');
  protected readonly displayProjectName = computed(() => this.brandName() || 'Lebhas - Brand Attire');
  protected readonly compactRoleLabel = computed(() => this.roleBadgeLabel().slice(0, 1));

  protected isActive(item: NavigationItem): boolean {
    const candidates = [item.route, ...(item.activePaths ?? [])];

    return candidates.some((candidate) => {
      const normalizedCandidate = this.normalizeRoute(candidate);

      if (this.isParameterizedRoutePattern(normalizedCandidate)) {
        return this.matchesRoutePattern(this.activeRoute(), normalizedCandidate, this.activeMatch(item));
      }

      return this.matchesStaticRoute(this.activeRoute(), normalizedCandidate, this.activeMatch(item));
    });
  }

  protected routerLinkActiveOptions(item: NavigationItem): { exact: boolean } {
    return { exact: this.activeMatch(item) === 'exact' };
  }

  protected selectItem(): void {
    this.itemSelected.emit();
  }

  protected openBillingModal(): void {
    this.billingModal.show();
    this.selectItem();
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

  private routeFromNavigationEvent(
    event: NavigationStart | NavigationEnd | NavigationCancel | NavigationError | NavigationSkipped,
  ): string {
    if (event instanceof NavigationStart) {
      return event.url;
    }

    if (event instanceof NavigationEnd) {
      return event.urlAfterRedirects;
    }

    return this.router.url;
  }

  private isParameterizedRoutePattern(route: string): boolean {
    return route.split('/').some((segment) => segment.startsWith(':'));
  }

  private matchesStaticRoute(
    currentRoute: string,
    candidate: string,
    activeMatch: NavigationItem['activeMatch'],
  ): boolean {
    if (activeMatch === 'exact') {
      return currentRoute === candidate;
    }

    return currentRoute === candidate || currentRoute.startsWith(`${candidate}/`);
  }

  private activeMatch(item: NavigationItem): 'exact' | 'prefix' {
    if (typeof item.exact === 'boolean') {
      return item.exact ? 'exact' : 'prefix';
    }

    return item.activeMatch ?? 'exact';
  }

  private matchesRoutePattern(
    currentRoute: string,
    candidate: string,
    activeMatch: NavigationItem['activeMatch'],
  ): boolean {
    const regex = this.routePatternRegex(candidate, activeMatch);
    return regex.test(currentRoute);
  }

  private routePatternRegex(route: string, activeMatch: NavigationItem['activeMatch']): RegExp {
    const escapedSegments = route
      .split('/')
      .map((segment) => (segment.startsWith(':') ? '[^/]+' : this.escapeRegex(segment)))
      .join('/');
    const suffix = activeMatch === 'exact' ? '$' : '(?:/.*)?$';

    return new RegExp(`^${escapedSegments}${suffix}`);
  }

  private escapeRegex(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
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
