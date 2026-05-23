import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_enums.dart';
import 'package:lebhas_creative_maker/features/assets/state/asset_controller.dart';

class AssetFilterBar extends ConsumerWidget {
  const AssetFilterBar({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedType = ref.watch(assetTypeFilterProvider);
    final selectedCategory = ref.watch(assetCategoryFilterProvider);

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: [
          _FilterChip<AssetType?>(
            label: 'All Types',
            isSelected: selectedType == null,
            onSelected: (_) => ref.read(assetTypeFilterProvider.notifier).state = null,
          ),
          ...AssetType.values.map((type) => _FilterChip<AssetType>(
            label: type.name.toUpperCase(),
            isSelected: selectedType == type,
            onSelected: (_) => ref.read(assetTypeFilterProvider.notifier).state = type,
          )),
          const SizedBox(width: 16),
          Container(width: 1, height: 24, color: context.colorScheme.outlineVariant),
          const SizedBox(width: 16),
          _FilterChip<AssetCategory?>(
            label: 'All Categories',
            isSelected: selectedCategory == null,
            onSelected: (_) => ref.read(assetCategoryFilterProvider.notifier).state = null,
          ),
          ...AssetCategory.values.map((cat) => _FilterChip<AssetCategory>(
            label: cat.name.toUpperCase(),
            isSelected: selectedCategory == cat,
            onSelected: (_) => ref.read(assetCategoryFilterProvider.notifier).state = cat,
          )),
        ],
      ),
    );
  }
}

class _FilterChip<T> extends StatelessWidget {
  final String label;
  final bool isSelected;
  final ValueChanged<bool> onSelected;

  const _FilterChip({
    required this.label,
    required this.isSelected,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(
        label: Text(label, style: const TextStyle(fontSize: 12)),
        selected: isSelected,
        onSelected: onSelected,
        showCheckmark: false,
      ),
    );
  }
}
