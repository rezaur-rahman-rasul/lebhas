import 'package:freezed_annotation/freezed_annotation.dart';

part 'brand_models.freezed.dart';
part 'brand_models.g.dart';

@freezed
class Brand with _$Brand {
  const factory Brand({
    required String id,
    required String workspaceId,
    required String ownerUserId,
    required String name,
    required String businessType,
    required String industry,
    required String targetAudience,
    required String brandVoice,
    required String preferredCTA,
    String? primaryColor,
    String? secondaryColor,
    String? website,
    String? facebookUrl,
    String? instagramUrl,
    String? linkedinUrl,
    String? tiktokUrl,
    @Default('ACTIVE') String status,
    DateTime? createdAt,
  }) = _Brand;

  factory Brand.fromJson(Map<String, dynamic> json) => _$BrandFromJson(json);
}

@freezed
class CreateBrandRequest with _$CreateBrandRequest {
  const factory CreateBrandRequest({
    required String name,
    required String businessType,
    required String industry,
    required String targetAudience,
    required String brandVoice,
    required String preferredCTA,
    String? primaryColor,
    String? secondaryColor,
    String? website,
    String? facebookUrl,
    String? instagramUrl,
    String? linkedinUrl,
    String? tiktokUrl,
  }) = _CreateBrandRequest;

  factory CreateBrandRequest.fromJson(Map<String, dynamic> json) => _$CreateBrandRequestFromJson(json);
}
