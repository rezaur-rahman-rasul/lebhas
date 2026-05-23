import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../workspace/state/workspace_controller.dart';
import '../data/product_service_repository.dart';
import '../domain/product_service_models.dart';

final productServicesProvider = FutureProvider.family<List<ProductService>, String>((ref, brandId) async {
  final workspaceContext = ref.watch(workspaceContextProvider);
  if (workspaceContext == null) return [];
  
  final repository = ref.watch(productServiceRepositoryProvider);
  return repository.getProductServices(workspaceContext.workspaceId, brandId);
});

final productServiceDetailsProvider = FutureProvider.family<ProductService, String>((ref, productServiceId) async {
  final workspaceContext = ref.watch(workspaceContextProvider);
  if (workspaceContext == null) throw Exception('No workspace context');
  
  final repository = ref.watch(productServiceRepositoryProvider);
  return repository.getProductServiceDetails(workspaceContext.workspaceId, productServiceId);
});

class ProductServiceController extends StateNotifier<AsyncValue<void>> {
  final ProductServiceRepository _repository;
  final Ref _ref;

  ProductServiceController(this._repository, this._ref) : super(const AsyncValue.data(null));

  Future<void> createProductService(CreateProductServiceRequest request) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.createProductService(workspaceContext.workspaceId, request);
      _ref.invalidate(productServicesProvider(request.brandId));
    });
  }

  Future<void> updateProductService(String productServiceId, CreateProductServiceRequest request) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.updateProductService(workspaceContext.workspaceId, productServiceId, request);
      _ref.invalidate(productServicesProvider(request.brandId));
      _ref.invalidate(productServiceDetailsProvider(productServiceId));
    });
  }

  Future<void> deleteProductService(String brandId, String productServiceId) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.deleteProductService(workspaceContext.workspaceId, productServiceId);
      _ref.invalidate(productServicesProvider(brandId));
    });
  }
}

final productServiceControllerProvider = StateNotifierProvider<ProductServiceController, AsyncValue<void>>((ref) {
  final repository = ref.watch(productServiceRepositoryProvider);
  return ProductServiceController(repository, ref);
});
