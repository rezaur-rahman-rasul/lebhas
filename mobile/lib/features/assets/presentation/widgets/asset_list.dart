import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_models.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_enums.dart';

class AssetList extends StatelessWidget {
  final List<Asset> assets;
  final ScrollPhysics? physics;
  final bool shrinkWrap;

  const AssetList({
    super.key,
    required this.assets,
    this.physics,
    this.shrinkWrap = false,
  });

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      physics: physics,
      shrinkWrap: shrinkWrap,
      itemCount: assets.length,
      separatorBuilder: (context, index) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final asset = assets[index];
        return Card(
          clipBehavior: Clip.antiAlias,
          child: ListTile(
            onTap: () => context.push('/assets/${asset.id}'),
            leading: _buildLeading(context, asset),
            title: Text(
              asset.displayName,
              style: const TextStyle(fontWeight: FontWeight.bold),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            subtitle: Text(
              '${asset.assetCategory.name.toUpperCase()} • ${DateFormat('MMM dd').format(asset.createdAt)}',
              style: const TextStyle(fontSize: 12),
            ),
            trailing: const Icon(Icons.chevron_right, size: 20),
          ),
        );
      },
    );
  }

  Widget _buildLeading(BuildContext context, Asset asset) {
    if (asset.assetType == AssetType.image && asset.file?.cdnUrl != null) {
      return Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(8),
          image: DecorationImage(
            image: NetworkImage(asset.file!.cdnUrl!),
            fit: BoxFit.cover,
          ),
        ),
      );
    }

    IconData icon;
    switch (asset.assetType) {
      case AssetType.image:
        icon = Icons.image_outlined;
        break;
      case AssetType.video:
        icon = Icons.videocam_outlined;
        break;
      case AssetType.document:
        icon = Icons.description_outlined;
        break;
      default:
        icon = Icons.insert_drive_file_outlined;
    }

    return Container(
      width: 48,
      height: 48,
      decoration: BoxDecoration(
        color: context.colorScheme.primaryContainer.withValues(alpha: 0.2),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Icon(icon, color: context.colorScheme.primary, size: 24),
    );
  }
}
