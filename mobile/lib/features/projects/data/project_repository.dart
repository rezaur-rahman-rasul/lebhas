import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_response.dart';
import '../domain/project_models.dart';
import 'project_api.dart';

final projectRepositoryProvider = Provider<ProjectRepository>((ref) {
  final projectApi = ref.watch(projectApiProvider);
  return ProjectRepository(projectApi);
});

class ProjectRepository {
  final ProjectApi _projectApi;

  ProjectRepository(this._projectApi);

  Future<List<ProjectCampaign>> getProjects(String workspaceId) async {
    final rawResponse = await _projectApi.getProjects(workspaceId);
    final response = ApiResponse<List<ProjectCampaign>>.fromJson(
      rawResponse,
      (json) => (json as List).map((e) => ProjectCampaign.fromJson(e as Map<String, dynamic>)).toList(),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load projects');
    }
  }

  Future<ProjectCampaign> createProject(String workspaceId, CreateProjectRequest request) async {
    final rawResponse = await _projectApi.createProject(workspaceId, request);
    final response = ApiResponse<ProjectCampaign>.fromJson(
      rawResponse,
      (json) => ProjectCampaign.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to create project');
    }
  }

  Future<ProjectCampaign> getProjectDetails(String workspaceId, String projectId) async {
    final rawResponse = await _projectApi.getProjectDetails(workspaceId, projectId);
    final response = ApiResponse<ProjectCampaign>.fromJson(
      rawResponse,
      (json) => ProjectCampaign.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to load project details');
    }
  }

  Future<ProjectCampaign> updateProject(String workspaceId, String projectId, CreateProjectRequest request) async {
    final rawResponse = await _projectApi.updateProject(workspaceId, projectId, request);
    final response = ApiResponse<ProjectCampaign>.fromJson(
      rawResponse,
      (json) => ProjectCampaign.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      return response.data!;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to update project');
    }
  }

  Future<void> deleteProject(String workspaceId, String projectId) async {
    final rawResponse = await _projectApi.deleteProject(workspaceId, projectId);
    final response = ApiResponse<dynamic>.fromJson(rawResponse, (json) => json);

    if (!response.success) {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Failed to delete project');
    }
  }
}
