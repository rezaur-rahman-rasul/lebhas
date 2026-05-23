import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_enums.dart';

part 'asset_models.freezed.dart';
part 'asset_models.g.dart';

@freezed
class StorageFile with _$StorageFile {
  const factory StorageFile({
    required String id,
    required String workspaceId,
    required String provider,
    required String bucket,
    required String objectKey,
    String? cdnUrl,
    required String mimeType,
    required String fileExtension,
    required int fileSize,
    String? hash,
    int? width,
    int? height,
    int? duration,
    @Default('STANDARD') String storageClass,
    required String filePurpose,
    required DateTime createdAt,
    required DateTime updatedAt,
  }) = _StorageFile;

  factory StorageFile.fromJson(Map<String, dynamic> json) => _$StorageFileFromJson(json);
}

@freezed
class Asset with _$Asset {
  const factory Asset({
    required String id,
    required String workspaceId,
    required String brandId,
    required String productServiceId,
    required String projectCampaignId,
    required String storageFileId,
    required String uploadedBy,
    required AssetType assetType,
    required AssetCategory assetCategory,
    required String originalFileName,
    required String displayName,
    String? description,
    @Default([]) List<String> tags,
    @Default(AssetStatus.active) AssetStatus status,
    required DateTime createdAt,
    required DateTime updatedAt,
    StorageFile? file,
  }) = _Asset;

  factory Asset.fromJson(Map<String, dynamic> json) => _$AssetFromJson(json);
}

@freezed
class AssetUploadRequest with _$AssetUploadRequest {
  const factory AssetUploadRequest({
    required AssetCategory assetCategory,
    required String displayName,
    String? description,
    @Default([]) List<String> tags,
  }) = _AssetUploadRequest;

  factory AssetUploadRequest.fromJson(Map<String, dynamic> json) => _$AssetUploadRequestFromJson(json);
}
