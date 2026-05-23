import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_models.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_enums.dart';

class AssetPreview extends StatelessWidget {
  final Asset asset;
  final double? height;
  final double width;

  const AssetPreview({
    super.key,
    required this.asset,
    this.height,
    this.width = double.infinity,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height ?? 300,
      decoration: BoxDecoration(
        color: context.colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
        borderRadius: BorderRadius.circular(16),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: _buildPreviewContent(context),
      ),
    );
  }

  Widget _buildPreviewContent(BuildContext context) {
    if (asset.assetType == AssetType.image && asset.file?.cdnUrl != null) {
      return Image.network(
        asset.file!.cdnUrl!,
        fit: BoxFit.contain,
        loadingBuilder: (context, child, loadingProgress) {
          if (loadingProgress == null) return child;
          return const Center(child: CircularProgressIndicator());
        },
        errorBuilder: (_, __, ___) => const _PlaceholderIcon(icon: Icons.broken_image_outlined),
      );
    }

    if (asset.assetType == AssetType.video) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.play_circle_outline, size: 64),
            SizedBox(height: 12),
            Text('Video Preview'),
          ],
        ),
      );
    }

    IconData icon;
    switch (asset.assetType) {
      case AssetType.document:
        icon = Icons.description_outlined;
        break;
      default:
        icon = Icons.insert_drive_file_outlined;
    }

    return _PlaceholderIcon(icon: icon);
  }
}

class _PlaceholderIcon extends StatelessWidget {
  final IconData icon;
  const _PlaceholderIcon({required this.icon});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Icon(
        icon,
        size: 64,
        color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.5),
      ),
    );
  }
}
