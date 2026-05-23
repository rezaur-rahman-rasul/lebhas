import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_client.dart';

final workspaceApiProvider = Provider<WorkspaceApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return WorkspaceApi(apiClient);
});

class WorkspaceApi {
  final ApiClient _apiClient;

  WorkspaceApi(this._apiClient);

  Future<Map<String, dynamic>> getMyWorkspaces() async {
    final response = await _apiClient.get('/workspaces/me');
    return response.data;
  }

  Future<Map<String, dynamic>> getWorkspaceContext(String workspaceId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/context');
    return response.data;
  }
}
