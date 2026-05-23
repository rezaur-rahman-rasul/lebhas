import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';

class PlatformStrip extends StatelessWidget {
  const PlatformStrip({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 24),
      color: context.colorScheme.surface.withValues(alpha: 0.5),
      child: Column(
        children: [
          Text(
            context.l10n.supportedPlatforms,
            style: context.textTheme.labelSmall?.copyWith(
              letterSpacing: 2,
              color: context.colorScheme.primary,
            ),
          ),
          const SizedBox(height: 16),
          const SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _PlatformIcon(name: 'Facebook', icon: Icons.facebook),
                _PlatformIcon(name: 'Instagram', icon: Icons.camera_alt_outlined),
                _PlatformIcon(name: 'TikTok', icon: Icons.music_note),
                _PlatformIcon(name: 'LinkedIn', icon: Icons.business),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PlatformIcon extends StatelessWidget {
  final String name;
  final IconData icon;

  const _PlatformIcon({required this.name, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        children: [
          Icon(
            icon,
            size: 20,
            color: context.textTheme.bodyMedium?.color?.withValues(alpha: 0.5),
          ),
          const SizedBox(width: 8),
          Text(
            name,
            style: context.textTheme.bodyMedium?.copyWith(
              fontWeight: FontWeight.w600,
              color: context.textTheme.bodyMedium?.color?.withValues(alpha: 0.5),
            ),
          ),
        ],
      ),
    );
  }
}
