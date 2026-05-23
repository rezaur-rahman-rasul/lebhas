import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/features/workspace/state/workspace_controller.dart';
import 'package:lebhas_creative_maker/features/assets/data/asset_repository.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_enums.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_models.dart';

final projectAssetsProvider = FutureProvider.family<List<Asset>, String>((ref, projectId) async {
  final workspaceContext = ref.watch(workspaceContextProvider);
  if (workspaceContext == null) return [];
  
  final repository = ref.watch(assetRepositoryProvider);
  return repository.getProjectAssets(workspaceContext.workspaceId, projectId);
});

final assetDetailsProvider = FutureProvider.family<Asset, String>((ref, assetId) async {
  final workspaceContext = ref.watch(workspaceContextProvider);
  if (workspaceContext == null) throw Exception('No workspace context');
  
  final repository = ref.watch(assetRepositoryProvider);
  return repository.getAssetDetails(workspaceContext.workspaceId, assetId);
});

class AssetController extends StateNotifier<AsyncValue<void>> {
  final AssetRepository _repository;
  final Ref _ref;

  AssetController(this._repository, this._ref) : super(const AsyncValue.data(null));

  Future<void> uploadAsset({
    required String projectId,
    required AssetUploadRequest request,
    required MultipartFile file,
  }) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();

    state = await AsyncValue.guard(() async {
      await _repository.uploadAsset(
        workspaceId: workspaceContext.workspaceId,
        projectId: projectId,
        request: request,
        file: file,
      );
      _ref.invalidate(projectAssetsProvider(projectId));
    });
  }

  Future<void> deleteAsset(String projectId, String assetId) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.deleteAsset(workspaceContext.workspaceId, assetId);
      _ref.invalidate(projectAssetsProvider(projectId));
    });
  }
}

final assetControllerProvider = StateNotifierProvider<AssetController, AsyncValue<void>>((ref) {
  final repository = ref.watch(assetRepositoryProvider);
  return AssetController(repository, ref);
});

final assetTypeFilterProvider = StateProvider<AssetType?>((ref) => null);
final assetCategoryFilterProvider = StateProvider<AssetCategory?>((ref) => null);
final assetSearchQueryProvider = StateProvider<String>((ref) => '');

final filteredProjectAssetsProvider = FutureProvider.family<List<Asset>, String>((ref, projectId) async {
  final assets = await ref.watch(projectAssetsProvider(projectId).future);
  final typeFilter = ref.watch(assetTypeFilterProvider);
  final categoryFilter = ref.watch(assetCategoryFilterProvider);
  final searchQuery = ref.watch(assetSearchQueryProvider).toLowerCase();

  return assets.where((asset) {
    final matchesType = typeFilter == null || asset.assetType == typeFilter;
    final matchesCategory = categoryFilter == null || asset.assetCategory == categoryFilter;
    final matchesSearch = searchQuery.isEmpty || 
        asset.displayName.toLowerCase().contains(searchQuery) || 
        asset.originalFileName.toLowerCase().contains(searchQuery);
    return matchesType && matchesCategory && matchesSearch;
  }).toList();
});
