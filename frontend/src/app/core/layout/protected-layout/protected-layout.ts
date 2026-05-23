import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { LayoutStateService } from '@app/core/state/layout-state.service';
import { LoadingStateService } from '@app/core/state/loading-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { IconComponent } from '@app/shared/components/icon/icon';
import { LoadingComponent } from '@app/shared/components/loading/loading';
import { SidebarComponent } from '@app/shared/components/sidebar/sidebar';
import { SpinnerComponent } from '@app/shared/components/spinner/spinner';
import { NavigationItem } from '@app/shared/layouts/navigation.models';
import { ThemeToggleComponent } from '@app/shared/components/theme-toggle/theme-toggle';
import { UserProfileDropdownComponent } from '../user-profile-dropdown/user-profile-dropdown';

@Component({
  selector: 'app-protected-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    IconComponent,
    LoadingComponent,
    SidebarComponent,
    SpinnerComponent,
    ThemeToggleComponent,
    UserProfileDropdownComponent,
  ],
  templateUrl: './protected-layout.html',
  styleUrl: './protected-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProtectedLayoutComponent {
  protected readonly layout = inject(LayoutStateService);
  protected readonly auth = inject(CurrentUserStore);
  protected readonly loading = inject(LoadingStateService);
  protected readonly workspace = inject(WorkspaceStore);

  private readonly masterNavigation: readonly NavigationItem[] = [
    { label: 'System Dashboard', icon: 'layout-dashboard', route: '/dashboard' },
    { label: 'Workspaces', icon: 'building-2', route: '/workspaces' },
    { label: 'Users', icon: 'users', route: '/users' },
    { label: 'Support Access', icon: 'life-buoy', route: '/support-access' },
  ];

  private readonly adminNavigation: readonly NavigationItem[] = [
    { label: 'Dashboard', icon: 'layout-dashboard', route: '/dashboard' },
    { label: 'Brands', icon: 'badge-check', route: '/brands' },
    { label: 'Products/Services', icon: 'package-open', route: '/product-services' },
    { label: 'Projects/Campaigns', icon: 'folder-kanban', route: '/projects' },
    { label: 'Assets', icon: 'image', route: '/assets' },
  ];

  private readonly crewNavigation: readonly NavigationItem[] = [
    { label: 'Dashboard', icon: 'layout-dashboard', route: '/dashboard' },
    { label: 'Assigned Projects', icon: 'folder-kanban', route: '/projects' },
    { label: 'Assets', icon: 'image', route: '/assets' },
  ];

  constructor() {
    void this.workspace.initialize();
  }

  protected readonly navigation = computed(() => {
    const role = this.auth.currentRole();
    switch (role) {
      case 'MASTER':
        return this.masterNavigation;
      case 'ADMIN':
        return this.adminNavigation;
      case 'CREW':
        return this.crewNavigation;
      default:
        return this.adminNavigation;
    }
  });
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly workspaceLabel = this.workspace.workspaceLabel;
  protected readonly workspaceOptions = this.workspace.workspaces;

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

  protected changeWorkspace(event: Event): void {
    const nextWorkspaceId = (event.target as HTMLSelectElement).value || null;
    this.workspace.setActiveWorkspaceId(nextWorkspaceId);
  }

  protected closeSidebar(): void {
    this.layout.closeSidebar();
  }
}
