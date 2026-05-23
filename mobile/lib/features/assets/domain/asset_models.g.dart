// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'asset_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$StorageFileImpl _$$StorageFileImplFromJson(Map<String, dynamic> json) =>
    _$StorageFileImpl(
      id: json['id'] as String,
      workspaceId: json['workspaceId'] as String,
      provider: json['provider'] as String,
      bucket: json['bucket'] as String,
      objectKey: json['objectKey'] as String,
      cdnUrl: json['cdnUrl'] as String?,
      mimeType: json['mimeType'] as String,
      fileExtension: json['fileExtension'] as String,
      fileSize: (json['fileSize'] as num).toInt(),
      hash: json['hash'] as String?,
      width: (json['width'] as num?)?.toInt(),
      height: (json['height'] as num?)?.toInt(),
      duration: (json['duration'] as num?)?.toInt(),
      storageClass: json['storageClass'] as String? ?? 'STANDARD',
      filePurpose: json['filePurpose'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$StorageFileImplToJson(_$StorageFileImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'workspaceId': instance.workspaceId,
      'provider': instance.provider,
      'bucket': instance.bucket,
      'objectKey': instance.objectKey,
      'cdnUrl': instance.cdnUrl,
      'mimeType': instance.mimeType,
      'fileExtension': instance.fileExtension,
      'fileSize': instance.fileSize,
      'hash': instance.hash,
      'width': instance.width,
      'height': instance.height,
      'duration': instance.duration,
      'storageClass': instance.storageClass,
      'filePurpose': instance.filePurpose,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt.toIso8601String(),
    };

_$AssetImpl _$$AssetImplFromJson(Map<String, dynamic> json) => _$AssetImpl(
      id: json['id'] as String,
      workspaceId: json['workspaceId'] as String,
      brandId: json['brandId'] as String,
      productServiceId: json['productServiceId'] as String,
      projectCampaignId: json['projectCampaignId'] as String,
      storageFileId: json['storageFileId'] as String,
      uploadedBy: json['uploadedBy'] as String,
      assetType: $enumDecode(_$AssetTypeEnumMap, json['assetType']),
      assetCategory: $enumDecode(_$AssetCategoryEnumMap, json['assetCategory']),
      originalFileName: json['originalFileName'] as String,
      displayName: json['displayName'] as String,
      description: json['description'] as String?,
      tags:
          (json['tags'] as List<dynamic>?)?.map((e) => e as String).toList() ??
              const [],
      status: $enumDecodeNullable(_$AssetStatusEnumMap, json['status']) ??
          AssetStatus.active,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      file: json['file'] == null
          ? null
          : StorageFile.fromJson(json['file'] as Map<String, dynamic>),
    );

Map<String, dynamic> _$$AssetImplToJson(_$AssetImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'workspaceId': instance.workspaceId,
      'brandId': instance.brandId,
      'productServiceId': instance.productServiceId,
      'projectCampaignId': instance.projectCampaignId,
      'storageFileId': instance.storageFileId,
      'uploadedBy': instance.uploadedBy,
      'assetType': _$AssetTypeEnumMap[instance.assetType]!,
      'assetCategory': _$AssetCategoryEnumMap[instance.assetCategory]!,
      'originalFileName': instance.originalFileName,
      'displayName': instance.displayName,
      'description': instance.description,
      'tags': instance.tags,
      'status': _$AssetStatusEnumMap[instance.status]!,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt.toIso8601String(),
      'file': instance.file,
    };

const _$AssetTypeEnumMap = {
  AssetType.image: 'IMAGE',
  AssetType.video: 'VIDEO',
  AssetType.document: 'DOCUMENT',
  AssetType.other: 'OTHER',
};

const _$AssetCategoryEnumMap = {
  AssetCategory.raw: 'RAW',
  AssetCategory.logo: 'LOGO',
  AssetCategory.creative: 'CREATIVE',
};

const _$AssetStatusEnumMap = {
  AssetStatus.active: 'ACTIVE',
  AssetStatus.archived: 'ARCHIVED',
  AssetStatus.deleted: 'DELETED',
};

_$AssetUploadRequestImpl _$$AssetUploadRequestImplFromJson(
        Map<String, dynamic> json) =>
    _$AssetUploadRequestImpl(
      assetCategory: $enumDecode(_$AssetCategoryEnumMap, json['assetCategory']),
      displayName: json['displayName'] as String,
      description: json['description'] as String?,
      tags:
          (json['tags'] as List<dynamic>?)?.map((e) => e as String).toList() ??
              const [],
    );

Map<String, dynamic> _$$AssetUploadRequestImplToJson(
        _$AssetUploadRequestImpl instance) =>
    <String, dynamic>{
      'assetCategory': _$AssetCategoryEnumMap[instance.assetCategory]!,
      'displayName': instance.displayName,
      'description': instance.description,
      'tags': instance.tags,
    };
