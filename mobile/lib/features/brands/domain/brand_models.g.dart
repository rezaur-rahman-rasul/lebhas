// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'brand_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$BrandImpl _$$BrandImplFromJson(Map<String, dynamic> json) => _$BrandImpl(
      id: json['id'] as String,
      workspaceId: json['workspaceId'] as String,
      ownerUserId: json['ownerUserId'] as String,
      name: json['name'] as String,
      businessType: json['businessType'] as String,
      industry: json['industry'] as String,
      targetAudience: json['targetAudience'] as String,
      brandVoice: json['brandVoice'] as String,
      preferredCTA: json['preferredCTA'] as String,
      primaryColor: json['primaryColor'] as String?,
      secondaryColor: json['secondaryColor'] as String?,
      website: json['website'] as String?,
      facebookUrl: json['facebookUrl'] as String?,
      instagramUrl: json['instagramUrl'] as String?,
      linkedinUrl: json['linkedinUrl'] as String?,
      tiktokUrl: json['tiktokUrl'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$BrandImplToJson(_$BrandImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'workspaceId': instance.workspaceId,
      'ownerUserId': instance.ownerUserId,
      'name': instance.name,
      'businessType': instance.businessType,
      'industry': instance.industry,
      'targetAudience': instance.targetAudience,
      'brandVoice': instance.brandVoice,
      'preferredCTA': instance.preferredCTA,
      'primaryColor': instance.primaryColor,
      'secondaryColor': instance.secondaryColor,
      'website': instance.website,
      'facebookUrl': instance.facebookUrl,
      'instagramUrl': instance.instagramUrl,
      'linkedinUrl': instance.linkedinUrl,
      'tiktokUrl': instance.tiktokUrl,
      'status': instance.status,
      'createdAt': instance.createdAt?.toIso8601String(),
    };

_$CreateBrandRequestImpl _$$CreateBrandRequestImplFromJson(
        Map<String, dynamic> json) =>
    _$CreateBrandRequestImpl(
      name: json['name'] as String,
      businessType: json['businessType'] as String,
      industry: json['industry'] as String,
      targetAudience: json['targetAudience'] as String,
      brandVoice: json['brandVoice'] as String,
      preferredCTA: json['preferredCTA'] as String,
      primaryColor: json['primaryColor'] as String?,
      secondaryColor: json['secondaryColor'] as String?,
      website: json['website'] as String?,
      facebookUrl: json['facebookUrl'] as String?,
      instagramUrl: json['instagramUrl'] as String?,
      linkedinUrl: json['linkedinUrl'] as String?,
      tiktokUrl: json['tiktokUrl'] as String?,
    );

Map<String, dynamic> _$$CreateBrandRequestImplToJson(
        _$CreateBrandRequestImpl instance) =>
    <String, dynamic>{
      'name': instance.name,
      'businessType': instance.businessType,
      'industry': instance.industry,
      'targetAudience': instance.targetAudience,
      'brandVoice': instance.brandVoice,
      'preferredCTA': instance.preferredCTA,
      'primaryColor': instance.primaryColor,
      'secondaryColor': instance.secondaryColor,
      'website': instance.website,
      'facebookUrl': instance.facebookUrl,
      'instagramUrl': instance.instagramUrl,
      'linkedinUrl': instance.linkedinUrl,
      'tiktokUrl': instance.tiktokUrl,
    };
