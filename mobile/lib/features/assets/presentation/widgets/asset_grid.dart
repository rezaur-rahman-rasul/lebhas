import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_models.dart';
import 'package:lebhas_creative_maker/features/assets/presentation/widgets/asset_card.dart';

class AssetGrid extends StatelessWidget {
  final List<Asset> assets;
  final ScrollPhysics? physics;
  final bool shrinkWrap;

  const AssetGrid({
    super.key,
    required this.assets,
    this.physics,
    this.shrinkWrap = false,
  });

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      padding: const EdgeInsets.all(16),
      physics: physics,
      shrinkWrap: shrinkWrap,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 16,
        crossAxisSpacing: 16,
        childAspectRatio: 0.85,
      ),
      itemCount: assets.length,
      itemBuilder: (context, index) {
        final asset = assets[index];
        return AssetCard(
          asset: asset,
          onTap: () => context.push('/assets/${asset.id}'),
        );
      },
    );
  }
}
