import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';
import 'package:lebhas_creative_maker/features/projects/state/project_controller.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_empty_state.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_filter_bar.dart';

class AssetLibraryScreen extends ConsumerWidget {
  const AssetLibraryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final projectsAsync = ref.watch(projectsProvider);
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Asset Library'),
      ),
      body: projectsAsync.when(
        data: (projects) {
          if (projects.isEmpty) {
            return const AssetEmptyState();
          }

          return Column(
            children: [
              const AssetFilterBar(),
              Expanded(
                child: ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: projects.length,
                  itemBuilder: (context, index) {
                    final project = projects[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 16),
                      child: ListTile(
                        leading: const CircleAvatar(
                          child: Icon(Icons.folder_outlined),
                        ),
                        title: Text(
                          project.name,
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                        subtitle: const Text('Manage project assets'),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => context.push('/projects/${project.id}/assets'),
                      ),
                    );
                  },
                ),
              ),
            ],
          );
        },
        loading: () => const AppLoader(),
        error: (e, s) => AppErrorView(
          message: e.toString(),
          onRetry: () => ref.refresh(projectsProvider),
        ),
      ),
    );
  }
}
