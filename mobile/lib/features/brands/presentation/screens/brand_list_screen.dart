import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/brands/state/brand_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_empty_state.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';

class BrandListScreen extends ConsumerWidget {
  const BrandListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final brandsAsync = ref.watch(brandsProvider);
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      body: brandsAsync.when(
        data: (brands) {
          if (brands.isEmpty) {
            return AppEmptyState(
              title: 'No Brands Found',
              message: 'Create your first brand to start generating creatives.',
              icon: Icons.branding_watermark_outlined,
              actionLabel: permissions.canManageBrands ? 'Create Brand' : null,
              onActionPressed: () => context.push('/brands/new'),
            );
          }

          return RefreshIndicator(
            onRefresh: () => ref.refresh(brandsProvider.future),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: brands.length,
              itemBuilder: (context, index) {
                final brand = brands[index];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: AppCard(
                    onTap: () => context.push('/brands/${brand.id}'),
                    child: ListTile(
                      leading: CircleAvatar(
                        backgroundColor: context.colorScheme.primaryContainer,
                        child: Text(
                          brand.name.substring(0, 1).toUpperCase(),
                          style: TextStyle(color: context.colorScheme.onPrimaryContainer),
                        ),
                      ),
                      title: Text(brand.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                      subtitle: Text('${brand.industry} • ${brand.businessType}'),
                      trailing: const Icon(Icons.chevron_right),
                    ),
                  ),
                );
              },
            ),
          );
        },
        loading: () => const AppLoader(),
        error: (error, _) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.refresh(brandsProvider),
        ),
      ),
      floatingActionButton: permissions.canManageBrands
          ? FloatingActionButton.extended(
              onPressed: () => context.push('/brands/new'),
              icon: const Icon(Icons.add),
              label: const Text('New Brand'),
            )
          : null,
    );
  }
}
