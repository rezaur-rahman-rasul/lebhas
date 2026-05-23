import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/core/api/api_response.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_models.dart';
import 'package:lebhas_creative_maker/features/assets/data/asset_api.dart';

final assetRepositoryProvider = Provider<AssetRepository>((ref) {
  final assetApi = ref.watch(assetApiProvider);
  return AssetRepository(assetApi);
});

class AssetRepository {
  final AssetApi _assetApi;

  AssetRepository(this._assetApi);

  Future<Asset> uploadAsset({
    required String workspaceId,
    required String projectId,
    required AssetUploadRequest request,
    required MultipartFile file,
  }) async {
    final formData = FormData.fromMap({
      'file': file,
      'assetCategory': request.assetCategory.name.toUpperCase(),
      'displayName': request.displayName,
      if (request.description != null) 'description': request.description,
      'tags': request.tags,
    });

    final rawResponse = await _assetApi.uploadAsset(
      workspaceId: workspaceId,
      projectId: projectId,
      data: formData,
    );

    final response = ApiResponse<Asset>.fromJson(
      rawResponse,
      (json) => Asset.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Upload failed');
    }
  }

  Future<List<Asset>> getProjectAssets(String workspaceId, String projectId) async {
    final rawResponse = await _assetApi.getProjectAssets(workspaceId, projectId);
    final response = ApiResponse<List<Asset>>.fromJson(
      rawResponse,
      (json) => (json as List).map((e) => Asset.fromJson(e as Map<String, dynamic>)).toList(),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load assets');
    }
  }

  Future<Asset> getAssetDetails(String workspaceId, String assetId) async {
    final rawResponse = await _assetApi.getAssetDetails(workspaceId, assetId);
    final response = ApiResponse<Asset>.fromJson(
      rawResponse,
      (json) => Asset.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load asset details');
    }
  }

  Future<void> deleteAsset(String workspaceId, String assetId) async {
    final rawResponse = await _assetApi.deleteAsset(workspaceId, assetId);
    final response = ApiResponse<dynamic>.fromJson(rawResponse, (json) => json);

    if (!response.success) {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to delete asset');
    }
  }

  Future<String> getPreviewUrl(String workspaceId, String assetId) async {
    final rawResponse = await _assetApi.getPreviewUrl(workspaceId, assetId);
    final response = ApiResponse<String>.fromJson(rawResponse, (json) => json as String);
    if (response.success && response.data != null) return response.data!;
    throw Exception('Failed to get preview URL');
  }

  Future<String> getDownloadUrl(String workspaceId, String assetId) async {
    final rawResponse = await _assetApi.getDownloadUrl(workspaceId, assetId);
    final response = ApiResponse<String>.fromJson(rawResponse, (json) => json as String);
    if (response.success && response.data != null) return response.data!;
    throw Exception('Failed to get download URL');
  }
}
