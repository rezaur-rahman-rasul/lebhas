import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/workspace/state/workspace_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final workspaceContext = ref.watch(workspaceContextProvider);
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: () async {
          await ref.read(workspaceControllerProvider.notifier).loadWorkspaces();
        },
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Welcome, ${workspaceContext?.workspaceName ?? 'User'}',
                style: context.textTheme.displaySmall?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                'You are logged in as ${workspaceContext?.role}',
                style: context.textTheme.bodyLarge?.copyWith(
                  color: context.colorScheme.onSurface.withValues(alpha: 0.6),
                ),
              ),
              const SizedBox(height: 32),
              
              const _StatsGrid(),
              
              const SizedBox(height: 32),
              Text(
                'Quick Actions',
                style: context.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              _QuickActions(permissions: permissions),
              
              const SizedBox(height: 32),
              // Activity placeholder
              AppCard(
                child: Column(
                  children: [
                    ListTile(
                      leading: Icon(Icons.history, color: context.colorScheme.primary),
                      title: const Text('Recent Activity'),
                      subtitle: const Text('No recent activity in this workspace.'),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatsGrid extends StatelessWidget {
  const _StatsGrid();

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount: 2,
      mainAxisSpacing: 16,
      crossAxisSpacing: 16,
      childAspectRatio: 1.5,
      children: const [
        _StatCard(label: 'Brands', value: '0', icon: Icons.branding_watermark_outlined),
        _StatCard(label: 'Products', value: '0', icon: Icons.inventory_2_outlined),
        _StatCard(label: 'Projects', value: '0', icon: Icons.campaign_outlined),
        _StatCard(label: 'Creatives', value: '0', icon: Icons.auto_awesome_motion_outlined),
      ],
    );
  }
}

class _StatCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;

  const _StatCard({required this.label, required this.value, required this.icon});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Icon(icon, size: 20, color: context.colorScheme.primary),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                value,
                style: context.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
              ),
              Text(
                label,
                style: context.textTheme.labelMedium?.copyWith(
                  color: context.colorScheme.onSurface.withValues(alpha: 0.6),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _QuickActions extends StatelessWidget {
  final PermissionHelpers permissions;
  const _QuickActions({required this.permissions});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        if (permissions.canManageBrands)
          _ActionTile(
            title: 'Create Brand',
            subtitle: 'Set up a new brand profile',
            icon: Icons.add_business_outlined,
            onTap: () => context.push('/brands/new'),
          ),
        if (permissions.canManageProducts)
          _ActionTile(
            title: 'Add Product/Service',
            subtitle: 'Organize items under your brands',
            icon: Icons.add_box_outlined,
            onTap: () => context.push('/product-services/new'),
          ),
        if (permissions.canCreateProjects)
          _ActionTile(
            title: 'Start Campaign',
            subtitle: 'Launch a new creative project',
            icon: Icons.rocket_launch_outlined,
            onTap: () => context.push('/projects/new'),
          ),
      ],
    );
  }
}

class _ActionTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final VoidCallback onTap;

  const _ActionTile({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: AppCard(
        onTap: onTap,
        child: ListTile(
          leading: Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: context.colorScheme.primaryContainer.withValues(alpha: 0.3),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, color: context.colorScheme.primary),
          ),
          title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
          subtitle: Text(subtitle),
          trailing: const Icon(Icons.chevron_right, size: 20),
        ),
      ),
    );
  }
}
