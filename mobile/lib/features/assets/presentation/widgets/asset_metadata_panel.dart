import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_models.dart';

class AssetMetadataPanel extends StatelessWidget {
  final Asset asset;

  const AssetMetadataPanel({super.key, required this.asset});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Asset Metadata',
            style: context.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 20),
          _MetadataRow(label: 'Original Name', value: asset.originalFileName),
          const Divider(height: 24),
          _MetadataRow(label: 'Asset Type', value: asset.assetType.name.toUpperCase()),
          const Divider(height: 24),
          _MetadataRow(label: 'Category', value: asset.assetCategory.name.toUpperCase()),
          const Divider(height: 24),
          _MetadataRow(
            label: 'File Size', 
            value: _formatFileSize(asset.file?.fileSize ?? 0),
          ),
          if (asset.file?.mimeType != null) ...[
            const Divider(height: 24),
            _MetadataRow(label: 'MIME Type', value: asset.file!.mimeType),
          ],
          if (asset.file?.width != null && asset.file?.height != null) ...[
            const Divider(height: 24),
            _MetadataRow(label: 'Dimensions', value: '${asset.file!.width} x ${asset.file!.height} px'),
          ],
          const Divider(height: 24),
          _MetadataRow(
            label: 'Uploaded At', 
            value: DateFormat('MMM dd, yyyy HH:mm').format(asset.createdAt),
          ),
          const Divider(height: 24),
          _MetadataRow(label: 'Status', value: asset.status.name.toUpperCase()),
        ],
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
    return '${size.toStringAsFixed(2)} ${suffixes[unit]}';
  }
}

class _MetadataRow extends StatelessWidget {
  final String label;
  final String value;

  const _MetadataRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 120,
          child: Text(
            label,
            style: context.textTheme.labelMedium?.copyWith(
              color: context.colorScheme.onSurface.withValues(alpha: 0.6),
            ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: context.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
            textAlign: TextAlign.right,
          ),
        ),
      ],
    );
  }
}
