// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'product_service_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$ProductServiceImpl _$$ProductServiceImplFromJson(Map<String, dynamic> json) =>
    _$ProductServiceImpl(
      id: json['id'] as String,
      workspaceId: json['workspaceId'] as String,
      brandId: json['brandId'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      category: json['category'] as String,
      targetAudience: json['targetAudience'] as String,
      sellingPoints: json['sellingPoints'] as String,
      status: json['status'] as String? ?? 'ACTIVE',
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$ProductServiceImplToJson(
        _$ProductServiceImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'workspaceId': instance.workspaceId,
      'brandId': instance.brandId,
      'name': instance.name,
      'description': instance.description,
      'category': instance.category,
      'targetAudience': instance.targetAudience,
      'sellingPoints': instance.sellingPoints,
      'status': instance.status,
      'createdAt': instance.createdAt?.toIso8601String(),
    };

_$CreateProductServiceRequestImpl _$$CreateProductServiceRequestImplFromJson(
        Map<String, dynamic> json) =>
    _$CreateProductServiceRequestImpl(
      brandId: json['brandId'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      category: json['category'] as String,
      targetAudience: json['targetAudience'] as String,
      sellingPoints: json['sellingPoints'] as String,
    );

Map<String, dynamic> _$$CreateProductServiceRequestImplToJson(
        _$CreateProductServiceRequestImpl instance) =>
    <String, dynamic>{
      'brandId': instance.brandId,
      'name': instance.name,
      'description': instance.description,
      'category': instance.category,
      'targetAudience': instance.targetAudience,
      'sellingPoints': instance.sellingPoints,
    };
