import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_client.dart';
import '../domain/brand_models.dart';

final brandApiProvider = Provider<BrandApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return BrandApi(apiClient);
});

class BrandApi {
  final ApiClient _apiClient;

  BrandApi(this._apiClient);

  Future<Map<String, dynamic>> getBrands(String workspaceId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/brands');
    return response.data;
  }

  Future<Map<String, dynamic>> createBrand(String workspaceId, CreateBrandRequest request) async {
    final response = await _apiClient.post(
      '/workspaces/$workspaceId/brands',
      data: request.toJson(),
    );
    return response.data;
  }

  Future<Map<String, dynamic>> getBrandDetails(String workspaceId, String brandId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/brands/$brandId');
    return response.data;
  }

  Future<Map<String, dynamic>> updateBrand(String workspaceId, String brandId, CreateBrandRequest request) async {
    final response = await _apiClient.put(
      '/workspaces/$workspaceId/brands/$brandId',
      data: request.toJson(),
    );
    return response.data;
  }

  Future<Map<String, dynamic>> deleteBrand(String workspaceId, String brandId) async {
    final response = await _apiClient.delete('/workspaces/$workspaceId/brands/$brandId');
    return response.data;
  }
}
