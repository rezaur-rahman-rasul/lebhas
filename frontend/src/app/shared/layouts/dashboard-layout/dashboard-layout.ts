import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

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
  private readonly workspaceDropdownRef = viewChild<ElementRef<HTMLElement>>('workspaceDropdown');
  private readonly mobileWorkspaceDropdownRef = viewChild<ElementRef<HTMLElement>>('mobileWorkspaceDropdown');

  constructor() {
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

  protected readonly sidebarClasses = computed(() =>
    [
      'fixed inset-y-0 left-0 z-40 flex flex-col border-r border-border bg-surface transition-all duration-200 lg:translate-x-0',
      this.layout.sidebarCollapsed() ? 'lg:w-[4.75rem]' : 'lg:w-64',
      this.layout.sidebarOpen() ? 'w-64 translate-x-0' : 'w-64 -translate-x-full',
    ].join(' '),
  );

  protected readonly contentClasses = computed(() =>
    [
      'min-h-screen transition-[padding] duration-200',
      this.layout.sidebarCollapsed() ? 'lg:pl-[4.75rem]' : 'lg:pl-64',
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
