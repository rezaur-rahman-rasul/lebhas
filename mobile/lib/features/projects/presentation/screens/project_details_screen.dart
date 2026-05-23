import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/projects/state/project_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';

class ProjectDetailScreen extends ConsumerWidget {
  final String projectId;
  const ProjectDetailScreen({super.key, required this.projectId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final projectAsync = ref.watch(projectDetailsProvider(projectId));
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Project Details'),
        actions: [
          if (permissions.canUpdateProjects)
            IconButton(
              icon: const Icon(Icons.edit_outlined),
              onPressed: () => context.push('/projects/$projectId/edit'),
            ),
        ],
      ),
      body: projectAsync.when(
        data: (project) => RefreshIndicator(
          onRefresh: () async => ref.invalidate(projectDetailsProvider(projectId)),
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: context.colorScheme.primaryContainer.withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(
                        _getPlatformIcon(project.targetPlatform),
                        color: context.colorScheme.primary,
                        size: 32,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            project.name,
                            style: context.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                          ),
                          Text(
                            '${project.targetPlatform} • ${project.campaignType}',
                            style: context.textTheme.bodyMedium?.copyWith(
                              color: context.colorScheme.onSurface.withValues(alpha: 0.6),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 32),
                
                _InfoCard(
                  title: 'Campaign Objective',
                  content: project.campaignObjective,
                  icon: Icons.track_changes,
                ),
                const SizedBox(height: 16),
                _InfoCard(
                  title: 'Description/Brief',
                  content: project.description.isNotEmpty ? project.description : 'No description provided.',
                  icon: Icons.description_outlined,
                ),
                
                const SizedBox(height: 32),
                Text(
                  'Creative Generation',
                  style: context.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),
                AppCard(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      children: [
                        Icon(Icons.auto_awesome, size: 48, color: context.colorScheme.primary.withValues(alpha: 0.5)),
                        const SizedBox(height: 16),
                        const Text(
                          'Ready to generate creatives for this project?',
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 24),
                        ElevatedButton(
                          onPressed: () {
                            // Navigation to Creative Editor (Day 3+)
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Creative generation module coming soon!')),
                            );
                          },
                          child: const Text('Start Generating'),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        loading: () => const AppLoader(),
        error: (e, _) => AppErrorView(message: e.toString(), onRetry: () => ref.refresh(projectDetailsProvider(projectId))),
      ),
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

class _InfoCard extends StatelessWidget {
  final String title;
  final String content;
  final IconData icon;

  const _InfoCard({required this.title, required this.content, required this.icon});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 20, color: context.colorScheme.primary),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: context.textTheme.labelMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: context.colorScheme.onSurface.withValues(alpha: 0.6),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  content,
                  style: context.textTheme.bodyLarge,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
