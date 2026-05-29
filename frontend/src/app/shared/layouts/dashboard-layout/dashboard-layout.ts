import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { NavigationEnd, NavigationStart, Router, RouterLink, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { LayoutStateService } from '@app/core/state/layout-state.service';
import { LoadingStateService } from '@app/core/state/loading-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { IconComponent } from '@app/shared/components/icon/icon';
import { NavigationItem, NavigationPermissionKey } from '@app/shared/layouts/navigation.models';
import { ThemeToggleComponent } from '@app/shared/components/theme-toggle/theme-toggle';
import { UserProfileDropdownComponent } from '@app/core/layout/user-profile-dropdown/user-profile-dropdown';
import { UserRole } from '@app/features/auth/models/user.models';
import { NotificationStore } from '@app/features/notifications/state/notification.store';
import { BillingPurchaseModalComponent } from '@app/features/payments/components/billing-purchase-modal/billing-purchase-modal';
import { BillingModalService } from '@app/features/payments/services/billing-modal.service';
import { SidebarComponent } from './components/app-sidebar/app-sidebar';
import { DASHBOARD_NAVIGATION } from './dashboard-navigation';

@Component({
  selector: 'app-protected-layout',
  standalone: true,
  imports: [
    RouterLink,
    RouterOutlet,
    IconComponent,
    SidebarComponent,
    ThemeToggleComponent,
    UserProfileDropdownComponent,
    BillingPurchaseModalComponent,
  ],
  templateUrl: './dashboard-layout.html',
  styleUrl: './dashboard-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProtectedLayoutComponent {
  protected readonly layout = inject(LayoutStateService);
  protected readonly auth = inject(CurrentUserStore);
  protected readonly permissions = inject(PermissionStore);
  protected readonly loading = inject(LoadingStateService);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly notifications = inject(NotificationStore);
  protected readonly billingModal = inject(BillingModalService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly workspaceDropdownRef = viewChild<ElementRef<HTMLElement>>('workspaceDropdown');
  private readonly mobileWorkspaceDropdownRef = viewChild<ElementRef<HTMLElement>>('mobileWorkspaceDropdown');

  constructor() {
    this.router.events.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.resetDashboardScroll();
      }

      if (event instanceof NavigationEnd) {
        this.resetDashboardScroll();
        this.queueDashboardScrollReset();
      }
    });

    effect(() => {
      if (!this.auth.isAuthenticated()) {
        return;
      }

      void this.workspace.initialize();
    });

    effect(() => {
      const workspaceId = this.workspace.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewNotifications()) {
        return;
      }

      void this.notifications.loadUnreadCount(workspaceId);
    });
  }

  protected readonly navigation = computed(() => {
    const role = this.normalizeNavigationRole(this.auth.currentRole());
    return DASHBOARD_NAVIGATION.filter((item) => this.canShowNavigationItem(item, role));
  });
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly workspaceLabel = this.workspace.workspaceLabel;
  protected readonly workspaceOptions = this.workspace.workspaces;
  protected readonly workspaceDropdownOpen = signal(false);
  protected readonly workspaceSearch = signal('');
  protected readonly visibleWorkspaceOptions = computed(() => {
    const search = this.workspaceSearch().trim().toLowerCase();
    const options = this.workspaceOptions();

    if (!search) {
      return options;
    }

    return options.filter((option) => option.name.toLowerCase().includes(search));
  });
  protected readonly notificationBellLabel = computed(() => {
    const count = this.notifications.unreadCount();
    return count > 0 ? `Open notifications. ${count} unread.` : 'Open notifications.';
  });
  protected readonly creditsSummaryLabel = computed(() => {
    const remaining = this.workspace.remainingCreditsLabel();
    return remaining === 'Usage details unavailable' ? 'Credits unavailable' : `${remaining} credits`;
  });

  protected openBillingModal(): void {
    this.billingModal.show();
  }

  protected readonly sidebarClasses = computed(() =>
    [
      'fixed inset-y-0 left-0 z-40 flex flex-col border-r border-border/70 bg-surface transition-all duration-200 lg:translate-x-0',
      this.layout.sidebarCollapsed() ? 'lg:w-[var(--app-sidebar-compact-width)]' : 'lg:w-[var(--app-sidebar-width)]',
      this.layout.sidebarOpen() ? 'w-[var(--app-sidebar-width)] translate-x-0' : 'w-[var(--app-sidebar-width)] -translate-x-full',
    ].join(' '),
  );

  protected readonly contentClasses = computed(() =>
    [
      'min-h-screen transition-[padding] duration-200',
      this.layout.sidebarCollapsed() ? 'lg:pl-[var(--app-sidebar-compact-width)]' : 'lg:pl-[var(--app-sidebar-width)]',
    ].join(' '),
  );

  @HostListener('document:click', ['$event'])
  protected closeWorkspaceDropdownOnOutsideClick(event: MouseEvent): void {
    const target = event.target as Node;
    const host = this.workspaceDropdownRef()?.nativeElement;
    const mobileHost = this.mobileWorkspaceDropdownRef()?.nativeElement;

    if (host?.contains(target) || mobileHost?.contains(target)) {
      return;
    }

    if (host || mobileHost) {
      this.workspaceDropdownOpen.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  protected closeWorkspaceDropdownOnEscape(): void {
    this.workspaceDropdownOpen.set(false);
  }

  protected toggleWorkspaceDropdown(): void {
    if (this.workspace.loading()) {
      return;
    }

    this.workspaceDropdownOpen.update((open) => !open);
  }

  protected updateWorkspaceSearch(event: Event): void {
    this.workspaceSearch.set((event.target as HTMLInputElement).value);
  }

  protected selectWorkspace(nextWorkspaceId: string | null): void {
    this.workspace.setActiveWorkspaceId(nextWorkspaceId);
    this.workspaceSearch.set('');
    this.workspaceDropdownOpen.set(false);
  }

  protected closeSidebar(): void {
    this.layout.closeSidebar();
  }

  private queueDashboardScrollReset(): void {
    if (typeof window === 'undefined') {
      return;
    }

    window.requestAnimationFrame(() => this.resetDashboardScroll());
    window.setTimeout(() => this.resetDashboardScroll(), 0);
  }

  private resetDashboardScroll(): void {
    if (typeof window === 'undefined' || typeof document === 'undefined') {
      return;
    }

    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
    document.documentElement.scrollTop = 0;
    document.documentElement.scrollLeft = 0;
    document.body.scrollTop = 0;
    document.body.scrollLeft = 0;
    document.querySelector('main')?.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }

  private canShowNavigationItem(item: NavigationItem, role: UserRole): boolean {
    if (item.roles && !item.roles.includes(role)) {
      return false;
    }

    return item.permissionKey ? this.checkNavigationPermission(item.permissionKey) : true;
  }

  private checkNavigationPermission(permissionKey: NavigationPermissionKey): boolean {
    if (!this.navigationPermissionKeys.includes(permissionKey)) {
      return false;
    }

    return this.permissions[permissionKey]();
  }

  private normalizeNavigationRole(role: UserRole | null): UserRole {
    switch (role) {
      case 'MASTER':
        return 'MASTER';
      case 'ADMIN':
        return 'ADMIN';
      case 'CREW':
        return 'CREW';
      default:
        return 'ADMIN';
    }
  }

  private readonly navigationPermissionKeys: readonly NavigationPermissionKey[] = [
    'canViewBrands',
    'canViewProducts',
    'canViewProjects',
    'canViewAiMonitoring',
    'canViewProviderMetrics',
    'canViewLayerAnalytics',
    'canViewWorkspaceAiUsage',
    'canViewQualityScores',
    'canViewAiFailures',
    'canViewUsageBilling',
    'canViewCreditLedger',
    'canViewDownloadUsage',
    'canViewShareUsage',
    'canViewMasterUsage',
    'canViewPlanUtilization',
    'canManagePaymentProviders',
    'canManageCreditPackages',
    'canPurchaseSubscription',
    'canPurchaseCredits',
    'canViewPayments',
    'canViewInvoices',
    'canViewNotifications',
    'canManageNotificationPreferences',
    'canViewActivityFeed',
    'canViewAuditLogs',
    'canViewMasterMonitoring',
    'canViewSystemHealth',
    'canViewMonitoringAlerts',
  ];
}
