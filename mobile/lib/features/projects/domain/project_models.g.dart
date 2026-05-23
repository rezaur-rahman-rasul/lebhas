// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'project_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$ProjectCampaignImpl _$$ProjectCampaignImplFromJson(
        Map<String, dynamic> json) =>
    _$ProjectCampaignImpl(
      id: json['id'] as String,
      workspaceId: json['workspaceId'] as String,
      brandId: json['brandId'] as String,
      productServiceId: json['productServiceId'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      campaignObjective: json['campaignObjective'] as String,
      targetPlatform: json['targetPlatform'] as String,
      campaignType: json['campaignType'] as String,
      status: json['status'] as String? ?? 'ACTIVE',
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$ProjectCampaignImplToJson(
        _$ProjectCampaignImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'workspaceId': instance.workspaceId,
      'brandId': instance.brandId,
      'productServiceId': instance.productServiceId,
      'name': instance.name,
      'description': instance.description,
      'campaignObjective': instance.campaignObjective,
      'targetPlatform': instance.targetPlatform,
      'campaignType': instance.campaignType,
      'status': instance.status,
      'createdAt': instance.createdAt?.toIso8601String(),
    };

_$CreateProjectRequestImpl _$$CreateProjectRequestImplFromJson(
        Map<String, dynamic> json) =>
    _$CreateProjectRequestImpl(
      productServiceId: json['productServiceId'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      campaignObjective: json['campaignObjective'] as String,
      targetPlatform: json['targetPlatform'] as String,
      campaignType: json['campaignType'] as String,
    );

Map<String, dynamic> _$$CreateProjectRequestImplToJson(
        _$CreateProjectRequestImpl instance) =>
    <String, dynamic>{
      'productServiceId': instance.productServiceId,
      'name': instance.name,
      'description': instance.description,
      'campaignObjective': instance.campaignObjective,
      'targetPlatform': instance.targetPlatform,
      'campaignType': instance.campaignType,
    };
