import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_response.dart';
import '../domain/workspace_models.dart';
import 'workspace_api.dart';

final workspaceRepositoryProvider = Provider<WorkspaceRepository>((ref) {
  final workspaceApi = ref.watch(workspaceApiProvider);
  return WorkspaceRepository(workspaceApi);
});

class WorkspaceRepository {
  final WorkspaceApi _workspaceApi;

  WorkspaceRepository(this._workspaceApi);

  Future<List<Workspace>> getMyWorkspaces() async {
    final rawResponse = await _workspaceApi.getMyWorkspaces();
    final response = ApiResponse<List<Workspace>>.fromJson(
      rawResponse,
      (json) => (json as List).map((e) => Workspace.fromJson(e as Map<String, dynamic>)).toList(),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load workspaces');
    }
  }

  Future<WorkspaceContext> getWorkspaceContext(String workspaceId) async {
    final rawResponse = await _workspaceApi.getWorkspaceContext(workspaceId);
    final response = ApiResponse<WorkspaceContext>.fromJson(
      rawResponse,
      (json) => WorkspaceContext.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load workspace context');
    }
  }
}
