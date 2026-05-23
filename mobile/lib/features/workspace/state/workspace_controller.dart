import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/workspace_repository.dart';
import '../domain/workspace_models.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

part 'workspace_controller.freezed.dart';

@freezed
class WorkspaceState with _$WorkspaceState {
  const factory WorkspaceState.initial() = _Initial;
  const factory WorkspaceState.loading() = _Loading;
  const factory WorkspaceState.loaded({
    required List<Workspace> workspaces,
    Workspace? selectedWorkspace,
    WorkspaceContext? context,
  }) = _Loaded;
  const factory WorkspaceState.error(String message) = _Error;
}

class WorkspaceController extends StateNotifier<WorkspaceState> {
  final WorkspaceRepository _repository;

  WorkspaceController(this._repository) : super(const WorkspaceState.initial());

  Future<void> loadWorkspaces() async {
    state = const WorkspaceState.loading();
    try {
      final workspaces = await _repository.getMyWorkspaces();
      if (workspaces.isNotEmpty) {
        final selected = workspaces.firstWhere((w) => w.isDefault, orElse: () => workspaces.first);
        state = WorkspaceState.loaded(workspaces: workspaces, selectedWorkspace: selected);
        await loadWorkspaceContext(selected.id);
      } else {
        state = const WorkspaceState.loaded(workspaces: []);
      }
    } catch (e) {
      state = WorkspaceState.error(e.toString());
    }
  }

  Future<void> selectWorkspace(Workspace workspace) async {
    final currentState = state;
    if (currentState is _Loaded) {
      state = currentState.copyWith(selectedWorkspace: workspace, context: null);
      await loadWorkspaceContext(workspace.id);
    }
  }

  Future<void> loadWorkspaceContext(String workspaceId) async {
    try {
      final context = await _repository.getWorkspaceContext(workspaceId);
      final currentState = state;
      if (currentState is _Loaded) {
        state = currentState.copyWith(context: context);
      }
    } catch (e) {
      // Handle error
    }
  }
}

final workspaceControllerProvider = StateNotifierProvider<WorkspaceController, WorkspaceState>((ref) {
  final repository = ref.watch(workspaceRepositoryProvider);
  return WorkspaceController(repository);
});

final workspaceContextProvider = Provider<WorkspaceContext?>((ref) {
  final workspaceState = ref.watch(workspaceControllerProvider);
  return workspaceState.maybeWhen(
    loaded: (_, __, context) => context,
    orElse: () => null,
  );
});
