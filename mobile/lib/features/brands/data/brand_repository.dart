import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_response.dart';
import '../domain/brand_models.dart';
import 'brand_api.dart';

final brandRepositoryProvider = Provider<BrandRepository>((ref) {
  final brandApi = ref.watch(brandApiProvider);
  return BrandRepository(brandApi);
});

class BrandRepository {
  final BrandApi _brandApi;

  BrandRepository(this._brandApi);

  Future<List<Brand>> getBrands(String workspaceId) async {
    final rawResponse = await _brandApi.getBrands(workspaceId);
    final response = ApiResponse<List<Brand>>.fromJson(
      rawResponse,
      (json) => (json as List).map((e) => Brand.fromJson(e as Map<String, dynamic>)).toList(),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load brands');
    }
  }

  Future<Brand> createBrand(String workspaceId, CreateBrandRequest request) async {
    final rawResponse = await _brandApi.createBrand(workspaceId, request);
    final response = ApiResponse<Brand>.fromJson(
      rawResponse,
      (json) => Brand.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to create brand');
    }
  }

  Future<Brand> getBrandDetails(String workspaceId, String brandId) async {
    final rawResponse = await _brandApi.getBrandDetails(workspaceId, brandId);
    final response = ApiResponse<Brand>.fromJson(
      rawResponse,
      (json) => Brand.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load brand details');
    }
  }

  Future<Brand> updateBrand(String workspaceId, String brandId, CreateBrandRequest request) async {
    final rawResponse = await _brandApi.updateBrand(workspaceId, brandId, request);
    final response = ApiResponse<Brand>.fromJson(
      rawResponse,
      (json) => Brand.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to update brand');
    }
  }

  Future<void> deleteBrand(String workspaceId, String brandId) async {
    final rawResponse = await _brandApi.deleteBrand(workspaceId, brandId);
    final response = ApiResponse<dynamic>.fromJson(rawResponse, (json) => json);

    if (!response.success) {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to delete brand');
    }
  }
}
