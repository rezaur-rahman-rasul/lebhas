import 'package:freezed_annotation/freezed_annotation.dart';

part 'workspace_models.freezed.dart';
part 'workspace_models.g.dart';

@freezed
class WorkspaceContext with _$WorkspaceContext {
  const factory WorkspaceContext({
    required String workspaceId,
    required String workspaceName,
    required String role,
    @Default([]) List<String> permissions,
    @Default(false) bool supportMode,
  }) = _WorkspaceContext;

  factory WorkspaceContext.fromJson(Map<String, dynamic> json) => _$WorkspaceContextFromJson(json);
}

@freezed
class Workspace with _$Workspace {
  const factory Workspace({
    required String id,
    required String name,
    String? description,
    @Default(false) bool isDefault,
  }) = _Workspace;

  factory Workspace.fromJson(Map<String, dynamic> json) => _$WorkspaceFromJson(json);
}
