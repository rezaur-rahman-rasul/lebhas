import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../workspace/state/workspace_controller.dart';
import '../data/brand_repository.dart';
import '../domain/brand_models.dart';

final brandsProvider = FutureProvider<List<Brand>>((ref) async {
  final workspaceContext = ref.watch(workspaceContextProvider);
  if (workspaceContext == null) return [];
  
  final repository = ref.watch(brandRepositoryProvider);
  return repository.getBrands(workspaceContext.workspaceId);
});

final brandDetailsProvider = FutureProvider.family<Brand, String>((ref, brandId) async {
  final workspaceContext = ref.watch(workspaceContextProvider);
  if (workspaceContext == null) throw Exception('No workspace context');
  
  final repository = ref.watch(brandRepositoryProvider);
  return repository.getBrandDetails(workspaceContext.workspaceId, brandId);
});

class BrandController extends StateNotifier<AsyncValue<void>> {
  final BrandRepository _repository;
  final Ref _ref;

  BrandController(this._repository, this._ref) : super(const AsyncValue.data(null));

  Future<void> createBrand(CreateBrandRequest request) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.createBrand(workspaceContext.workspaceId, request);
      _ref.invalidate(brandsProvider);
    });
  }

  Future<void> updateBrand(String brandId, CreateBrandRequest request) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.updateBrand(workspaceContext.workspaceId, brandId, request);
      _ref.invalidate(brandsProvider);
      _ref.invalidate(brandDetailsProvider(brandId));
    });
  }

  Future<void> deleteBrand(String brandId) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.deleteBrand(workspaceContext.workspaceId, brandId);
      _ref.invalidate(brandsProvider);
    });
  }
}

final brandControllerProvider = StateNotifierProvider<BrandController, AsyncValue<void>>((ref) {
  final repository = ref.watch(brandRepositoryProvider);
  return BrandController(repository, ref);
});
