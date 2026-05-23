import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_response.dart';
import '../domain/product_service_models.dart';
import 'product_service_api.dart';

final productServiceRepositoryProvider = Provider<ProductServiceRepository>((ref) {
  final productServiceApi = ref.watch(productServiceApiProvider);
  return ProductServiceRepository(productServiceApi);
});

class ProductServiceRepository {
  final ProductServiceApi _productServiceApi;

  ProductServiceRepository(this._productServiceApi);

  Future<List<ProductService>> getProductServices(String workspaceId, String brandId) async {
    final rawResponse = await _productServiceApi.getProductServices(workspaceId, brandId);
    final response = ApiResponse<List<ProductService>>.fromJson(
      rawResponse,
      (json) => (json as List).map((e) => ProductService.fromJson(e as Map<String, dynamic>)).toList(),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load product services');
    }
  }

  Future<ProductService> createProductService(String workspaceId, CreateProductServiceRequest request) async {
    final rawResponse = await _productServiceApi.createProductService(workspaceId, request);
    final response = ApiResponse<ProductService>.fromJson(
      rawResponse,
      (json) => ProductService.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to create product service');
    }
  }

  Future<ProductService> getProductServiceDetails(String workspaceId, String productServiceId) async {
    final rawResponse = await _productServiceApi.getProductServiceDetails(workspaceId, productServiceId);
    final response = ApiResponse<ProductService>.fromJson(
      rawResponse,
      (json) => ProductService.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load product service details');
    }
  }

  Future<ProductService> updateProductService(String workspaceId, String productServiceId, CreateProductServiceRequest request) async {
    final rawResponse = await _productServiceApi.updateProductService(workspaceId, productServiceId, request);
    final response = ApiResponse<ProductService>.fromJson(
      rawResponse,
      (json) => ProductService.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to update product service');
    }
  }

  Future<void> deleteProductService(String workspaceId, String productServiceId) async {
    final rawResponse = await _productServiceApi.deleteProductService(workspaceId, productServiceId);
    final response = ApiResponse<dynamic>.fromJson(rawResponse, (json) => json);

    if (!response.success) {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to delete product service');
    }
  }
}
