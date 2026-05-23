import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';

class FeatureSection extends StatelessWidget {
  const FeatureSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
      child: Column(
        children: [
          Text(
            context.l10n.featuresTitle,
            style: context.textTheme.displayMedium,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 32),
          GridView.count(
            crossAxisCount: 1, // Simple vertical list for mobile-first
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 16,
            childAspectRatio: 3,
            children: [
              _FeatureCard(
                title: context.l10n.feature1Title,
                description: context.l10n.feature1Desc,
                icon: Icons.auto_fix_high,
              ),
              _FeatureCard(
                title: context.l10n.feature2Title,
                description: context.l10n.feature2Desc,
                icon: Icons.folder_copy_outlined,
              ),
              _FeatureCard(
                title: context.l10n.feature3Title,
                description: context.l10n.feature3Desc,
                icon: Icons.lightbulb_outline,
              ),
              _FeatureCard(
                title: context.l10n.feature4Title,
                description: context.l10n.feature4Desc,
                icon: Icons.inventory_2_outlined,
              ),
              _FeatureCard(
                title: context.l10n.feature5Title,
                description: context.l10n.feature5Desc,
                icon: Icons.group_outlined,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  final String title;
  final String description;
  final IconData icon;

  const _FeatureCard({
    required this.title,
    required this.description,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: context.colorScheme.primary.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: context.colorScheme.primary),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    title,
                    style: context.textTheme.titleLarge?.copyWith(fontSize: 18),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    description,
                    style: context.textTheme.bodyMedium,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
