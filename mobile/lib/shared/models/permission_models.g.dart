// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'permission_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$PermissionSnapshotImpl _$$PermissionSnapshotImplFromJson(
        Map<String, dynamic> json) =>
    _$PermissionSnapshotImpl(
      globalPermissions: (json['globalPermissions'] as List<dynamic>)
          .map((e) => e as String)
          .toList(),
      workspacePermissions:
          (json['workspacePermissions'] as Map<String, dynamic>).map(
        (k, e) =>
            MapEntry(k, (e as List<dynamic>).map((e) => e as String).toList()),
      ),
    );

Map<String, dynamic> _$$PermissionSnapshotImplToJson(
        _$PermissionSnapshotImpl instance) =>
    <String, dynamic>{
      'globalPermissions': instance.globalPermissions,
      'workspacePermissions': instance.workspacePermissions,
    };
