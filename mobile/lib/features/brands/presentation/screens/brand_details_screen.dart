import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/brands/state/brand_controller.dart';
import 'package:lebhas_creative_maker/features/product_services/state/product_service_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';

class BrandDetailScreen extends ConsumerWidget {
  final String brandId;
  const BrandDetailScreen({super.key, required this.brandId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final brandAsync = ref.watch(brandDetailsProvider(brandId));
    final productsAsync = ref.watch(productServicesProvider(brandId));
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Brand Details'),
        actions: [
          if (permissions.canManageBrands)
            IconButton(
              icon: const Icon(Icons.edit_outlined),
              onPressed: () => context.push('/brands/$brandId/edit'),
            ),
        ],
      ),
      body: brandAsync.when(
        data: (brand) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(brandDetailsProvider(brandId));
            ref.invalidate(productServicesProvider(brandId));
          },
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _BrandHeader(brand: brand),
                const SizedBox(height: 32),
                _BrandInfoGrid(brand: brand),
                const SizedBox(height: 32),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Products & Services',
                      style: context.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    if (permissions.canManageProducts)
                      TextButton.icon(
                        onPressed: () => context.push('/product-services/new?brandId=$brandId'),
                        icon: const Icon(Icons.add, size: 18),
                        label: const Text('Add'),
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                productsAsync.when(
                  data: (products) => products.isEmpty
                      ? const AppCard(
                          child: Padding(
                            padding: EdgeInsets.all(24),
                            child: Center(child: Text('No products added yet.')),
                          ),
                        )
                      : ListView.builder(
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          itemCount: products.length,
                          itemBuilder: (context, index) {
                            final product = products[index];
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 12),
                              child: AppCard(
                                onTap: () => context.push('/product-services/${product.id}'),
                                child: ListTile(
                                  title: Text(product.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                                  subtitle: Text(product.category),
                                  trailing: const Icon(Icons.chevron_right, size: 20),
                                ),
                              ),
                            );
                          },
                        ),
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (e, _) => Text('Error loading products: $e'),
                ),
              ],
            ),
          ),
        ),
        loading: () => const AppLoader(),
        error: (e, _) => AppErrorView(message: e.toString(), onRetry: () => ref.refresh(brandDetailsProvider(brandId))),
      ),
    );
  }
}

class _BrandHeader extends StatelessWidget {
  final dynamic brand;
  const _BrandHeader({required this.brand});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        CircleAvatar(
          radius: 40,
          backgroundColor: context.colorScheme.primaryContainer,
          child: Text(
            brand.name.substring(0, 1).toUpperCase(),
            style: context.textTheme.displaySmall?.copyWith(
              color: context.colorScheme.onPrimaryContainer,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        const SizedBox(width: 20),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                brand.name,
                style: context.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 4),
              Text(
                '${brand.industry} • ${brand.businessType}',
                style: context.textTheme.bodyLarge?.copyWith(
                  color: context.colorScheme.onSurface.withValues(alpha: 0.6),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _BrandInfoGrid extends StatelessWidget {
  final dynamic brand;
  const _BrandInfoGrid({required this.brand});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          _InfoRow(label: 'Target Audience', value: brand.targetAudience),
          const Divider(height: 32),
          _InfoRow(label: 'Brand Voice', value: brand.brandVoice),
          const Divider(height: 32),
          _InfoRow(label: 'Preferred CTA', value: brand.preferredCTA),
          if (brand.website != null) ...[
            const Divider(height: 32),
            _InfoRow(label: 'Website', value: brand.website!),
          ],
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;
  const _InfoRow({required this.label, required this.value});

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
              color: context.colorScheme.primary,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: context.textTheme.bodyMedium,
          ),
        ),
      ],
    );
  }
}
