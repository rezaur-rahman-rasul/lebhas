import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_empty_state.dart';

class AssetEmptyState extends StatelessWidget {
  final VoidCallback? onUploadPressed;

  const AssetEmptyState({super.key, this.onUploadPressed});

  @override
  Widget build(BuildContext context) {
    return AppEmptyState(
      title: 'No Assets Found',
      message: 'Upload raw images, logos, or videos to start your creative project.',
      icon: Icons.photo_library_outlined,
      actionLabel: onUploadPressed != null ? 'Upload Asset' : null,
      onActionPressed: onUploadPressed,
    );
  }
}
