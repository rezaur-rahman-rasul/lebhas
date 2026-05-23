import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/core/theme/theme_controller.dart';
import 'package:lebhas_creative_maker/core/localization/locale_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/features/auth/presentation/widgets/login_bottom_sheet.dart';

class PublicAppBar extends ConsumerWidget implements PreferredSizeWidget {
  const PublicAppBar({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeControllerProvider);
    final locale = ref.watch(localeControllerProvider);
    
    return AppBar(
      title: Text(
        context.l10n.appTitle,
        style: context.textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.bold,
          color: context.colorScheme.primary,
        ),
      ),
      actions: [
        // Language Toggle
        TextButton(
          onPressed: () => ref.read(localeControllerProvider.notifier).toggleLocale(),
          child: Text(
            locale.languageCode == 'en' ? 'BN' : 'EN',
            style: TextStyle(fontWeight: FontWeight.bold, color: context.colorScheme.primary),
          ),
        ),
        // Theme Toggle
        IconButton(
          icon: Icon(
            themeMode == ThemeMode.dark ? Icons.light_mode : Icons.dark_mode,
          ),
          onPressed: () => ref.read(themeControllerProvider.notifier).toggleTheme(),
        ),
        const SizedBox(width: 4),
        Padding(
          padding: const EdgeInsets.only(right: 16),
          child: TextButton(
            onPressed: () {
              showModalBottomSheet(
                context: context,
                isScrollControlled: true,
                backgroundColor: Colors.transparent,
                builder: (context) => const LoginBottomSheet(),
              );
            },
            child: Text(context.l10n.login),
          ),
        ),
      ],
    );
  }

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);
}
