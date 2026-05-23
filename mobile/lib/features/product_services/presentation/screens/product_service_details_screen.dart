import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/product_services/state/product_service_controller.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_card.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_loader.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_error_view.dart';
import 'package:lebhas_creative_maker/core/permissions/permission_helpers.dart';

class ProductServiceDetailScreen extends ConsumerWidget {
  final String productServiceId;
  const ProductServiceDetailScreen({super.key, required this.productServiceId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final productAsync = ref.watch(productServiceDetailsProvider(productServiceId));
    final permissions = ref.watch(permissionsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Product Details'),
        actions: [
          if (permissions.canManageProducts)
            IconButton(
              icon: const Icon(Icons.edit_outlined),
              onPressed: () => context.push('/product-services/$productServiceId/edit'),
            ),
        ],
      ),
      body: productAsync.when(
        data: (product) => RefreshIndicator(
          onRefresh: () async => ref.invalidate(productServiceDetailsProvider(productServiceId)),
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  product.name,
                  style: context.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
                Text(
                  product.category,
                  style: context.textTheme.titleMedium?.copyWith(
                    color: context.colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 24),
                
                _DetailSection(title: 'Description', content: product.description),
                const SizedBox(height: 24),
                _DetailSection(title: 'Target Audience', content: product.targetAudience),
                const SizedBox(height: 24),
                _DetailSection(title: 'Key Selling Points', content: product.sellingPoints),
                
                const SizedBox(height: 40),
                AppButton(
                  text: 'Create Campaign for this Product',
                  onPressed: () => context.push('/projects/new?productServiceId=${product.id}'),
                ),
              ],
            ),
          ),
        ),
        loading: () => const AppLoader(),
        error: (e, _) => AppErrorView(message: e.toString(), onRetry: () => ref.refresh(productServiceDetailsProvider(productServiceId))),
      ),
    );
  }
}

class _DetailSection extends StatelessWidget {
  final String title;
  final String content;
  const _DetailSection({required this.title, required this.content});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: context.textTheme.labelLarge?.copyWith(
            fontWeight: FontWeight.bold,
            color: context.colorScheme.onSurface.withValues(alpha: 0.6),
          ),
        ),
        const SizedBox(height: 8),
        AppCard(
          padding: const EdgeInsets.all(16),
          child: SizedBox(
            width: double.infinity,
            child: Text(
              content,
              style: context.textTheme.bodyLarge,
            ),
          ),
        ),
      ],
    );
  }
}

// Re-using AppButton for consistency
class AppButton extends StatelessWidget {
  final String text;
  final VoidCallback onPressed;
  const AppButton({super.key, required this.text, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        minimumSize: const Size(double.infinity, 56),
      ),
      child: Text(text),
    );
  }
}
