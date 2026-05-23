import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_client.dart';
import '../domain/project_models.dart';

final projectApiProvider = Provider<ProjectApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ProjectApi(apiClient);
});

class ProjectApi {
  final ApiClient _apiClient;

  ProjectApi(this._apiClient);

  Future<Map<String, dynamic>> getProjects(String workspaceId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/projects');
    return response.data;
  }

  Future<Map<String, dynamic>> createProject(String workspaceId, CreateProjectRequest request) async {
    final response = await _apiClient.post(
      '/workspaces/$workspaceId/product-services/${request.productServiceId}/projects',
      data: request.toJson(),
    );
    return response.data;
  }

  Future<Map<String, dynamic>> getProjectDetails(String workspaceId, String projectId) async {
    final response = await _apiClient.get('/workspaces/$workspaceId/projects/$projectId');
    return response.data;
  }

  Future<Map<String, dynamic>> updateProject(String workspaceId, String projectId, CreateProjectRequest request) async {
    final response = await _apiClient.put(
      '/workspaces/$workspaceId/projects/$projectId',
      data: request.toJson(),
    );
    return response.data;
  }

  Future<Map<String, dynamic>> deleteProject(String workspaceId, String projectId) async {
    final response = await _apiClient.delete('/workspaces/$workspaceId/projects/$projectId');
    return response.data;
  }
}
