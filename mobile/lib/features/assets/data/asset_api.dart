import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/core/api/api_client.dart';

final assetApiProvider = Provider<AssetApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return AssetApi(apiClient);
});

class AssetApi {
  final ApiClient _apiClient;

  AssetApi(this._apiClient);

  Future<Map<String, dynamic>> uploadAsset({
    required String workspaceId,
    required String projectId,
    required FormData data,
  }) async {
    final response = await _apiClient.post(
      '/workspaces/$workspaceId/projects/$projectId/assets/upload',
      data: data,
    );
    return response.data;
  }

  Future<Map<String, dynamic>> getProjectAssets(String workspaceId, String projectId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/projects/$projectId/assets');
    return response.data;
  }

  Future<Map<String, dynamic>> getAssetDetails(String workspaceId, String assetId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/assets/$assetId');
    return response.data;
  }

  Future<Map<String, dynamic>> deleteAsset(String workspaceId, String assetId) async {
    final response = await _apiClient.delete('/workspaces/$workspaceId/assets/$assetId');
    return response.data;
  }

  Future<Map<String, dynamic>> getPreviewUrl(String workspaceId, String assetId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/assets/$assetId/preview-url');
    return response.data;
  }

  Future<Map<String, dynamic>> getDownloadUrl(String workspaceId, String assetId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/assets/$assetId/download-url');
    return response.data;
  }
}
