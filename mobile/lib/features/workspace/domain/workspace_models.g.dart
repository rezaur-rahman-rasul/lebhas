// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'workspace_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$WorkspaceContextImpl _$$WorkspaceContextImplFromJson(
        Map<String, dynamic> json) =>
    _$WorkspaceContextImpl(
      workspaceId: json['workspaceId'] as String,
      workspaceName: json['workspaceName'] as String,
      role: json['role'] as String,
      permissions: (json['permissions'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          const [],
      supportMode: json['supportMode'] as bool? ?? false,
    );

Map<String, dynamic> _$$WorkspaceContextImplToJson(
        _$WorkspaceContextImpl instance) =>
    <String, dynamic>{
      'workspaceId': instance.workspaceId,
      'workspaceName': instance.workspaceName,
      'role': instance.role,
      'permissions': instance.permissions,
      'supportMode': instance.supportMode,
    };

_$WorkspaceImpl _$$WorkspaceImplFromJson(Map<String, dynamic> json) =>
    _$WorkspaceImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      isDefault: json['isDefault'] as bool? ?? false,
    );

Map<String, dynamic> _$$WorkspaceImplToJson(_$WorkspaceImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'isDefault': instance.isDefault,
    };
