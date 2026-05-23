import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_models.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_enums.dart';

class AssetCard extends StatelessWidget {
  final Asset asset;
  final VoidCallback? onTap;

  const AssetCard({
    super.key,
    required this.asset,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return AppCard(
      onTap: onTap,
      padding: EdgeInsets.zero,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AspectRatio(
            aspectRatio: 1,
            child: Container(
              decoration: BoxDecoration(
                color: context.colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
                borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
              ),
              child: _buildAssetPreview(context),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  asset.displayName,
                  style: context.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    _buildCategoryBadge(context),
                    const Spacer(),
                    Text(
                      _formatFileSize(asset.file?.fileSize ?? 0),
                      style: context.textTheme.labelSmall?.copyWith(
                        color: context.colorScheme.onSurface.withValues(alpha: 0.6),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAssetPreview(BuildContext context) {
    if (asset.assetType == AssetType.image && asset.file?.cdnUrl != null) {
      return ClipRRect(
        borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
        child: Image.network(
          asset.file!.cdnUrl!,
          fit: BoxFit.cover,
          errorBuilder: (_, __, ___) => const Icon(Icons.broken_image_outlined),
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

    return Center(
      child: Icon(
        icon,
        size: 32,
        color: context.colorScheme.primary.withValues(alpha: 0.5),
      ),
    );
  }

  Widget _buildCategoryBadge(BuildContext context) {
    Color color = Colors.grey;
    switch (asset.assetCategory) {
      case AssetCategory.raw:
        color = Colors.blue;
        break;
      case AssetCategory.logo:
        color = Colors.orange;
        break;
      case AssetCategory.creative:
        color = Colors.purple;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withValues(alpha: 0.5)),
      ),
      child: Text(
        asset.assetCategory.name.toUpperCase(),
        style: context.textTheme.labelSmall?.copyWith(
          color: color,
          fontSize: 8,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  String _formatFileSize(int bytes) {
    if (bytes <= 0) return '0 B';
    const suffixes = ['B', 'KB', 'MB', 'GB'];
    double size = bytes.toDouble();
    int unit = 0;
    while (size >= 1024 && unit < suffixes.length - 1) {
      size /= 1024;
      unit++;
    }
    return '${size.toStringAsFixed(1)} ${suffixes[unit]}';
  }
}
