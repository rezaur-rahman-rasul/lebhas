import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/projects/state/project_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_empty_state.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';

class ProjectListScreen extends ConsumerWidget {
  const ProjectListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final projectsAsync = ref.watch(projectsProvider);
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Projects & Campaigns'),
      ),
      body: projectsAsync.when(
        data: (projects) {
          if (projects.isEmpty) {
            return AppEmptyState(
              title: 'No Projects Found',
              message: 'Start a new campaign to generate high-converting creatives.',
              icon: Icons.campaign_outlined,
              actionLabel: permissions.canCreateProjects ? 'New Project' : null,
              onActionPressed: () => context.push('/projects/new'),
            );
          }

          return RefreshIndicator(
            onRefresh: () => ref.refresh(projectsProvider.future),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: projects.length,
              itemBuilder: (context, index) {
                final project = projects[index];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: AppCard(
                    onTap: () => context.push('/projects/${project.id}'),
                    child: ListTile(
                      leading: Icon(
                        _getPlatformIcon(project.targetPlatform),
                        color: context.colorScheme.primary,
                      ),
                      title: Text(project.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                      subtitle: Text('${project.campaignType} • ${project.targetPlatform}'),
                      trailing: const Icon(Icons.chevron_right),
                    ),
                  ),
                );
              },
            ),
          );
        },
        loading: () => const AppLoader(),
        error: (error, _) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.refresh(projectsProvider),
        ),
      ),
      floatingActionButton: permissions.canCreateProjects
          ? FloatingActionButton.extended(
              onPressed: () => context.push('/projects/new'),
              icon: const Icon(Icons.add),
              label: const Text('New Project'),
            )
          : null,
    );
  }

  IconData _getPlatformIcon(String platform) {
    switch (platform.toLowerCase()) {
      case 'facebook': return Icons.facebook;
      case 'instagram': return Icons.camera_alt_outlined;
      case 'tiktok': return Icons.music_note;
      case 'linkedin': return Icons.business;
      default: return Icons.rocket_launch_outlined;
    }
  }
}
