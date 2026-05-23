import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_preview.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_metadata_panel.dart';
import 'package:lebhas_creative_maker/features/assets/state/asset_controller.dart';
import 'package:lebhas_creative_maker/features/assets/data/asset_repository.dart';
import 'package:lebhas_creative_maker/features/workspace/state/workspace_controller.dart';

class AssetDetailScreen extends ConsumerWidget {
  final String assetId;
  const AssetDetailScreen({super.key, required this.assetId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final assetAsync = ref.watch(assetDetailsProvider(assetId));
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Asset Details'),
        actions: [
          if (permissions.canDownloadAssets)
            IconButton(
              icon: const Icon(Icons.download_outlined),
              onPressed: () => _downloadAsset(context, ref, assetId),
            ),
          if (permissions.canDeleteAssets)
            IconButton(
              icon: Icon(Icons.delete_outline, color: context.colorScheme.error),
              onPressed: () => _confirmDelete(context, ref, assetId),
            ),
        ],
      ),
      body: assetAsync.when(
        data: (asset) => RefreshIndicator(
          onRefresh: () async => ref.invalidate(assetDetailsProvider(assetId)),
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                AssetPreview(asset: asset),
                const SizedBox(height: 24),
                Text(
                  asset.displayName,
                  style: context.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                ),
                if (asset.description != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    asset.description!,
                    style: context.textTheme.bodyMedium?.copyWith(
                      color: context.colorScheme.onSurface.withValues(alpha: 0.7),
                    ),
                  ),
                ],
                const SizedBox(height: 32),
                AssetMetadataPanel(asset: asset),
                const SizedBox(height: 40),
              ],
            ),
          ),
        ),
        loading: () => const AppLoader(),
        error: (e, s) => AppErrorView(
          message: e.toString(),
          onRetry: () => ref.refresh(assetDetailsProvider(assetId)),
        ),
      ),
    );
  }

  Future<void> _downloadAsset(BuildContext context, WidgetRef ref, String assetId) async {
    try {
      final repository = ref.read(assetRepositoryProvider);
      final workspaceId = ref.read(workspaceContextProvider)?.workspaceId;
      if (workspaceId == null) return;

      final downloadUrl = await repository.getDownloadUrl(workspaceId, assetId);
      context.showSnackBar('Download link ready: $downloadUrl');
    } catch (e) {
      context.showSnackBar('Failed to get download link');
    }
  }

  Future<void> _confirmDelete(BuildContext context, WidgetRef ref, String assetId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Asset'),
        content: const Text('Are you sure you want to delete this asset? This action cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: context.colorScheme.error),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      final asset = ref.read(assetDetailsProvider(assetId)).value;
      if (asset == null) return;

      await ref.read(assetControllerProvider.notifier).deleteAsset(asset.projectCampaignId, assetId);
      if (context.mounted) Navigator.pop(context);
    }
  }
}
