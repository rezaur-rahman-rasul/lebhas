import 'package:freezed_annotation/freezed_annotation.dart';

part 'permission_models.freezed.dart';
part 'permission_models.g.dart';

@freezed
class PermissionSnapshot with _$PermissionSnapshot {
  const factory PermissionSnapshot({
    required List<String> globalPermissions,
    required Map<String, List<String>> workspacePermissions,
  }) = _PermissionSnapshot;

  factory PermissionSnapshot.fromJson(Map<String, dynamic> json) => _$PermissionSnapshotFromJson(json);
}
