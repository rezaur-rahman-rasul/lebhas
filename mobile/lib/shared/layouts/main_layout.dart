import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/auth/state/auth_controller.dart';
import 'package:lebhas_creative_maker/features/workspace/state/workspace_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';

class MainLayout extends ConsumerWidget {
  final Widget child;

  const MainLayout({
    super.key,
    required this.child,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final workspaceContext = ref.watch(workspaceContextProvider);
    final authState = ref.watch(authProvider);
    final permissions = ref.watch(permissionsProvider);
    
    final user = authState.maybeWhen(
      authenticated: (user) => user,
      orElse: () => null,
    );

    if (workspaceContext == null) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Lebhas Creative',
              style: context.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
            Text(
              '${workspaceContext.workspaceName} • ${workspaceContext.role}',
              style: context.textTheme.labelSmall?.copyWith(
                color: context.colorScheme.onSurface.withValues(alpha: 0.6),
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications_outlined),
            onPressed: () {},
          ),
          _UserAvatar(user: user),
        ],
      ),
      drawer: _AppDrawer(permissions: permissions),
      body: child,
      bottomNavigationBar: _BottomNavBar(permissions: permissions),
    );
  }
}

class _UserAvatar extends ConsumerWidget {
  final dynamic user;
  const _UserAvatar({required this.user});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.only(right: 16, left: 8),
      child: PopupMenuButton(
        offset: const Offset(0, 48),
        child: CircleAvatar(
          radius: 16,
          backgroundColor: context.colorScheme.primaryContainer,
          child: Text(
            user?.firstName.substring(0, 1).toUpperCase() ?? 'U',
            style: TextStyle(
              fontSize: 12,
              color: context.colorScheme.onPrimaryContainer,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        itemBuilder: (context) => [
          PopupMenuItem(
            child: ListTile(
              leading: const Icon(Icons.person_outline),
              title: const Text('Profile'),
              onTap: () => context.pop(),
            ),
          ),
          PopupMenuItem(
            child: ListTile(
              leading: Icon(Icons.logout, color: context.colorScheme.error),
              title: Text('Logout', style: TextStyle(color: context.colorScheme.error)),
              onTap: () {
                context.pop();
                ref.read(authProvider.notifier).logout();
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _AppDrawer extends StatelessWidget {
  final PermissionHelpers permissions;

  const _AppDrawer({required this.permissions});

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: Column(
        children: [
          DrawerHeader(
            decoration: BoxDecoration(
              color: context.colorScheme.primaryContainer.withValues(alpha: 0.1),
            ),
            child: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.auto_awesome, size: 48, color: context.colorScheme.primary),
                  const SizedBox(height: 12),
                  Text(
                    'Lebhas Creative',
                    style: context.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                  ),
                ],
              ),
            ),
          ),
          Expanded(
            child: ListView(
              padding: EdgeInsets.zero,
              children: [
                _buildDrawerItem(context, Icons.dashboard_outlined, 'Dashboard', '/dashboard'),
                
                if (permissions.isMaster) ...[
                   const Divider(),
                   _buildSectionHeader(context, 'MASTER ADMIN'),
                   _buildDrawerItem(context, Icons.workspaces_outline, 'Workspaces', '/workspaces'),
                   _buildDrawerItem(context, Icons.people_outline, 'Users', '/users'),
                   _buildDrawerItem(context, Icons.support_agent_outlined, 'Support Access', '/support'),
                ],

                if (permissions.isAdmin || permissions.isMaster) ...[
                  const Divider(),
                  _buildSectionHeader(context, 'MANAGEMENT'),
                  _buildDrawerItem(context, Icons.branding_watermark_outlined, 'Brands', '/brands'),
                  _buildDrawerItem(context, Icons.inventory_2_outlined, 'Products/Services', '/product-services'),
                  _buildDrawerItem(context, Icons.campaign_outlined, 'Projects/Campaigns', '/projects'),
                  _buildDrawerItem(context, Icons.photo_library_outlined, 'Asset Library', '/assets'),
                ],

                const Divider(),
                _buildSectionHeader(context, 'CREATIVE WORKFLOW'),
                _buildDrawerItem(context, Icons.add_photo_alternate_outlined, 'Creative Requests', '/creative-requests'),
                _buildDrawerItem(context, Icons.auto_awesome_motion_outlined, 'Generated Versions', '/generated-versions'),
                
                if (permissions.isAdmin) ...[
                  const Divider(),
                  _buildSectionHeader(context, 'ADMINISTRATION'),
                  _buildDrawerItem(context, Icons.payments_outlined, 'Usage & Billing', '/billing'),
                ],
                
                const Divider(),
                _buildDrawerItem(context, Icons.settings_outlined, 'Settings', '/settings'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Text(
        title,
        style: context.textTheme.labelSmall?.copyWith(
          fontWeight: FontWeight.bold,
          color: context.colorScheme.primary,
          letterSpacing: 1.2,
        ),
      ),
    );
  }

  Widget _buildDrawerItem(BuildContext context, IconData icon, String title, String route) {
    final bool isSelected = GoRouterState.of(context).uri.toString().startsWith(route);
    return ListTile(
      leading: Icon(icon, color: isSelected ? context.colorScheme.primary : null),
      title: Text(
        title,
        style: TextStyle(
          fontWeight: isSelected ? FontWeight.bold : null,
          color: isSelected ? context.colorScheme.primary : null,
        ),
      ),
      selected: isSelected,
      onTap: () {
        context.pop();
        context.go(route);
      },
    );
  }
}

class _BottomNavBar extends StatelessWidget {
  final PermissionHelpers permissions;

  const _BottomNavBar({required this.permissions});

  @override
  Widget build(BuildContext context) {
    final String location = GoRouterState.of(context).uri.toString();
    
    int currentIndex = 0;
    if (location.startsWith('/dashboard')) {
      currentIndex = 0;
    } else if (location.startsWith('/brands')) {
      currentIndex = 1;
    } else if (location.startsWith('/product-services')) {
      currentIndex = 2;
    } else if (location.startsWith('/projects')) {
      currentIndex = 3;
    }

    return NavigationBar(
      selectedIndex: currentIndex,
      onDestinationSelected: (index) {
        switch (index) {
          case 0:
            context.go('/dashboard');
            break;
          case 1:
            context.go('/brands');
            break;
          case 2:
            context.go('/product-services');
            break;
          case 3:
            context.go('/projects');
            break;
        }
      },
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.dashboard_outlined),
          selectedIcon: Icon(Icons.dashboard),
          label: 'Dashboard',
        ),
        NavigationDestination(
          icon: Icon(Icons.branding_watermark_outlined),
          selectedIcon: Icon(Icons.branding_watermark),
          label: 'Brands',
        ),
        NavigationDestination(
          icon: Icon(Icons.inventory_2_outlined),
          selectedIcon: Icon(Icons.inventory_2),
          label: 'Products',
        ),
        NavigationDestination(
          icon: Icon(Icons.campaign_outlined),
          selectedIcon: Icon(Icons.campaign),
          label: 'Projects',
        ),
      ],
    );
  }
}
