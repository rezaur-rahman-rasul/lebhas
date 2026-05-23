import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../workspace/state/workspace_controller.dart';
import '../data/project_repository.dart';
import '../domain/project_models.dart';

final projectsProvider = FutureProvider<List<ProjectCampaign>>((ref) async {
  final workspaceContext = ref.watch(workspaceContextProvider);
  if (workspaceContext == null) return [];
  
  final repository = ref.watch(projectRepositoryProvider);
  return repository.getProjects(workspaceContext.workspaceId);
});

final projectDetailsProvider = FutureProvider.family<ProjectCampaign, String>((ref, projectId) async {
  final workspaceContext = ref.watch(workspaceContextProvider);
  if (workspaceContext == null) throw Exception('No workspace context');
  
  final repository = ref.watch(projectRepositoryProvider);
  return repository.getProjectDetails(workspaceContext.workspaceId, projectId);
});

class ProjectController extends StateNotifier<AsyncValue<void>> {
  final ProjectRepository _repository;
  final Ref _ref;

  ProjectController(this._repository, this._ref) : super(const AsyncValue.data(null));

  Future<void> createProject(CreateProjectRequest request) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.createProject(workspaceContext.workspaceId, request);
      _ref.invalidate(projectsProvider);
    });
  }

  Future<void> updateProject(String projectId, CreateProjectRequest request) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.updateProject(workspaceContext.workspaceId, projectId, request);
      _ref.invalidate(projectsProvider);
      _ref.invalidate(projectDetailsProvider(projectId));
    });
  }

  Future<void> deleteProject(String projectId) async {
    final workspaceContext = _ref.read(workspaceContextProvider);
    if (workspaceContext == null) return;

    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      await _repository.deleteProject(workspaceContext.workspaceId, projectId);
      _ref.invalidate(projectsProvider);
    });
  }
}

final projectControllerProvider = StateNotifierProvider<ProjectController, AsyncValue<void>>((ref) {
  final repository = ref.watch(projectRepositoryProvider);
  return ProjectController(repository, ref);
});
