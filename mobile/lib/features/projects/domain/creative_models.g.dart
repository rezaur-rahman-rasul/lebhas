// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'creative_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$CreativeRequestImpl _$$CreativeRequestImplFromJson(
        Map<String, dynamic> json) =>
    _$CreativeRequestImpl(
      id: json['id'] as String,
      projectId: json['projectId'] as String,
      title: json['title'] as String,
      prompt: json['prompt'] as String,
      negativePrompt: json['negativePrompt'] as String?,
      aspectRatio: json['aspectRatio'] as String,
      status: json['status'] as String? ?? 'PENDING',
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$CreativeRequestImplToJson(
        _$CreativeRequestImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'projectId': instance.projectId,
      'title': instance.title,
      'prompt': instance.prompt,
      'negativePrompt': instance.negativePrompt,
      'aspectRatio': instance.aspectRatio,
      'status': instance.status,
      'createdAt': instance.createdAt?.toIso8601String(),
    };

_$GeneratedVersionImpl _$$GeneratedVersionImplFromJson(
        Map<String, dynamic> json) =>
    _$GeneratedVersionImpl(
      id: json['id'] as String,
      requestId: json['requestId'] as String,
      imageUrl: json['imageUrl'] as String,
      status: json['status'] as String? ?? 'DRAFT',
      isApproved: json['isApproved'] as bool? ?? false,
      score: (json['score'] as num?)?.toDouble(),
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$GeneratedVersionImplToJson(
        _$GeneratedVersionImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'requestId': instance.requestId,
      'imageUrl': instance.imageUrl,
      'status': instance.status,
      'isApproved': instance.isApproved,
      'score': instance.score,
      'createdAt': instance.createdAt?.toIso8601String(),
    };
