import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_client.dart';
import '../domain/product_service_models.dart';

final productServiceApiProvider = Provider<ProductServiceApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ProductServiceApi(apiClient);
});

class ProductServiceApi {
  final ApiClient _apiClient;

  ProductServiceApi(this._apiClient);

  Future<Map<String, dynamic>> getProductServices(String workspaceId, String brandId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/brands/$brandId/product-services');
    return response.data;
  }

  Future<Map<String, dynamic>> createProductService(String workspaceId, CreateProductServiceRequest request) async {
    final response = await _apiClient.post(
      '/workspaces/$workspaceId/brands/${request.brandId}/product-services',
      data: request.toJson(),
    );
    return response.data;
  }

  Future<Map<String, dynamic>> getProductServiceDetails(String workspaceId, String productServiceId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/product-services/$productServiceId');
    return response.data;
  }

  Future<Map<String, dynamic>> updateProductService(String workspaceId, String productServiceId, CreateProductServiceRequest request) async {
    final response = await _apiClient.put(
      '/workspaces/$workspaceId/product-services/$productServiceId',
      data: request.toJson(),
    );
    return response.data;
  }

  Future<Map<String, dynamic>> deleteProductService(String workspaceId, String productServiceId) async {
    final response = await _apiClient.delete('/workspaces/$workspaceId/product-services/$productServiceId');
    return response.data;
  }
}
