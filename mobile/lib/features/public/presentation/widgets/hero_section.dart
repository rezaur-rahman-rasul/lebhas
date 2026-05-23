import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_button.dart';
import 'package:lebhas_creative_maker/features/auth/presentation/widgets/login_bottom_sheet.dart';

class HeroSection extends StatelessWidget {
  const HeroSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
      child: Column(
        children: [
          Text(
            context.l10n.heroTitle,
            style: context.textTheme.displayLarge,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          Text(
            context.l10n.heroSubtitle,
            style: context.textTheme.bodyLarge?.copyWith(
              color: context.textTheme.bodyMedium?.color,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 32),
          AppButton(
            text: context.l10n.heroCta,
            onPressed: () {
              showModalBottomSheet(
                context: context,
                isScrollControlled: true,
                backgroundColor: Colors.transparent,
                builder: (context) => const LoginBottomSheet(),
              );
            },
          ),
          const SizedBox(height: 48),
          // Visual Placeholder with AI glow effect
          AspectRatio(
            aspectRatio: 16 / 9,
            child: Container(
              decoration: BoxDecoration(
                color: context.colorScheme.surface,
                borderRadius: BorderRadius.circular(24),
                border: Border.all(color: context.colorScheme.primary.withValues(alpha: 0.2)),
                boxShadow: [
                  BoxShadow(
                    color: context.colorScheme.primary.withValues(alpha: 0.1),
                    blurRadius: 40,
                    offset: const Offset(0, 20),
                  ),
                ],
              ),
              child: Stack(
                children: [
                  Positioned.fill(
                    child: Center(
                      child: Icon(
                        Icons.auto_awesome,
                        size: 64,
                        color: context.colorScheme.primary.withValues(alpha: 0.3),
                      ),
                    ),
                  ),
                  // Decorative Glows
                  Positioned(
                    top: -20,
                    right: -20,
                    child: _GlowCircle(color: context.colorScheme.primary),
                  ),
                  Positioned(
                    bottom: -20,
                    left: -20,
                    child: _GlowCircle(color: context.colorScheme.secondary),
                  ),
                  Positioned(
                    bottom: 24,
                    left: 24,
                    right: 24,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        _ImageBadge(
                          label: context.l10n.rawImage,
                          icon: Icons.photo_library_outlined,
                        ),
                        Icon(Icons.arrow_forward, color: context.colorScheme.primary),
                        _ImageBadge(
                          label: context.l10n.aiCreative,
                          icon: Icons.auto_fix_high,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _GlowCircle extends StatelessWidget {
  final Color color;
  const _GlowCircle({required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 100,
      height: 100,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        boxShadow: [
          BoxShadow(
            color: color.withValues(alpha: 0.1),
            blurRadius: 60,
            spreadRadius: 10,
          ),
        ],
      ),
    );
  }
}

class _ImageBadge extends StatelessWidget {
  final String label;
  final IconData icon;

  const _ImageBadge({required this.label, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.1)),
      ),
      child: Row(
        children: [
          Icon(icon, size: 16, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 8),
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
}
