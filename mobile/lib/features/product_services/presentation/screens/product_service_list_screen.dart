import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/brands/state/brand_controller.dart';
import 'package:lebhas_creative_maker/features/product_services/state/product_service_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_empty_state.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';

class ProductServiceListScreen extends ConsumerStatefulWidget {
  const ProductServiceListScreen({super.key});

  @override
  ConsumerState<ProductServiceListScreen> createState() => _ProductServiceListScreenState();
}

class _ProductServiceListScreenState extends ConsumerState<ProductServiceListScreen> {
  String? selectedBrandId;

  @override
  Widget build(BuildContext context) {
    final brandsAsync = ref.watch(brandsProvider);
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Products & Services'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(60),
          child: brandsAsync.when(
            data: (brands) {
              if (brands.isEmpty) return const SizedBox.shrink();
              return Container(
                height: 50,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: brands.length + 1,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (context, index) {
                    if (index == 0) {
                      return ChoiceChip(
                        label: const Text('All Brands'),
                        selected: selectedBrandId == null,
                        onSelected: (selected) {
                          setState(() => selectedBrandId = null);
                        },
                      );
                    }
                    final brand = brands[index - 1];
                    return ChoiceChip(
                      label: Text(brand.name),
                      selected: selectedBrandId == brand.id,
                      onSelected: (selected) {
                        setState(() => selectedBrandId = selected ? brand.id : null);
                      },
                    );
                  },
                ),
              );
            },
            loading: () => const SizedBox.shrink(),
            error: (_, __) => const SizedBox.shrink(),
          ),
        ),
      ),
      body: selectedBrandId == null
          ? _AllProductsView(permissions: permissions)
          : _BrandProductsView(brandId: selectedBrandId!, permissions: permissions),
      floatingActionButton: permissions.canManageProducts
          ? FloatingActionButton.extended(
              onPressed: () => context.push('/product-services/new'),
              icon: const Icon(Icons.add),
              label: const Text('New Product'),
            )
          : null,
    );
  }
}

class _AllProductsView extends ConsumerWidget {
  final PermissionHelpers permissions;
  const _AllProductsView({required this.permissions});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // For "All Products", we might need a separate provider or iterate through brands
    // For simplicity in Day 2, let's assume if brand is not selected, we show a prompt
    return AppEmptyState(
      title: 'Select a Brand',
      message: 'Choose a brand above to view its products and services.',
      icon: Icons.inventory_2_outlined,
    );
  }
}

class _BrandProductsView extends ConsumerWidget {
  final String brandId;
  final PermissionHelpers permissions;
  const _BrandProductsView({required this.brandId, required this.permissions});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final productsAsync = ref.watch(productServicesProvider(brandId));

    return productsAsync.when(
      data: (products) {
        if (products.isEmpty) {
          return AppEmptyState(
            title: 'No Products Found',
            message: 'Add products or services to this brand to start creating campaigns.',
            icon: Icons.inventory_2_outlined,
            actionLabel: permissions.canManageProducts ? 'Add Product' : null,
            onActionPressed: () => context.push('/product-services/new?brandId=$brandId'),
          );
        }

        return RefreshIndicator(
          onRefresh: () => ref.refresh(productServicesProvider(brandId).future),
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
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
        onRetry: () => ref.refresh(productServicesProvider(brandId)),
      ),
    );
  }
}
