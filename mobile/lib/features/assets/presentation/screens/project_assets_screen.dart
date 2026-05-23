import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';
import 'package:lebhas_creative_maker/features/projects/state/project_controller.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_empty_state.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_grid.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_filter_bar.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_upload_sheet.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/project_asset_context_card.dart';
import 'package:lebhas_creative_maker/features/assets/state/asset_controller.dart';

class ProjectAssetsScreen extends ConsumerWidget {
  final String projectId;
  const ProjectAssetsScreen({super.key, required this.projectId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final projectAsync = ref.watch(projectDetailsProvider(projectId));
    final assetsAsync = ref.watch(filteredProjectAssetsProvider(projectId));
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Project Assets'),
      ),
      body: projectAsync.when(
        data: (project) => Column(
          children: [
            ProjectAssetContextCard(project: project),
            const AssetFilterBar(),
            Expanded(
              child: assetsAsync.when(
                data: (assets) {
                  if (assets.isEmpty) {
                    return AssetEmptyState(
                      onUploadPressed: permissions.canUploadAssets 
                          ? () => _showUploadSheet(context, projectId) 
                          : null,
                    );
                  }
                  return RefreshIndicator(
                    onRefresh: () => ref.refresh(projectAssetsProvider(projectId).future),
                    child: AssetGrid(assets: assets),
                  );
                },
                loading: () => const AppLoader(),
                error: (e, s) => AppErrorView(
                  message: e.toString(),
                  onRetry: () => ref.refresh(projectAssetsProvider(projectId)),
                ),
              ),
            ),
          ],
        ),
        loading: () => const AppLoader(),
        error: (e, s) => AppErrorView(
          message: e.toString(),
          onRetry: () => ref.refresh(projectDetailsProvider(projectId)),
        ),
      ),
      floatingActionButton: permissions.canUploadAssets
          ? FloatingActionButton.extended(
              onPressed: () => _showUploadSheet(context, projectId),
              icon: const Icon(Icons.upload_file),
              label: const Text('Upload'),
            )
          : null,
    );
  }

  void _showUploadSheet(BuildContext context, String projectId) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => AssetUploadSheet(projectId: projectId),
    );
  }
}
