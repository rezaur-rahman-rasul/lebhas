import 'package:freezed_annotation/freezed_annotation.dart';

part 'creative_models.freezed.dart';
part 'creative_models.g.dart';

@freezed
class CreativeRequest with _$CreativeRequest {
  const factory CreativeRequest({
    required String id,
    required String projectId,
    required String title,
    required String prompt,
    String? negativePrompt,
    required String aspectRatio,
    @Default('PENDING') String status,
    DateTime? createdAt,
  }) = _CreativeRequest;

  factory CreativeRequest.fromJson(Map<String, dynamic> json) => _$CreativeRequestFromJson(json);
}

@freezed
class GeneratedVersion with _$GeneratedVersion {
  const factory GeneratedVersion({
    required String id,
    required String requestId,
    required String imageUrl,
    @Default('DRAFT') String status,
    @Default(false) bool isApproved,
    double? score,
    DateTime? createdAt,
  }) = _GeneratedVersion;

  factory GeneratedVersion.fromJson(Map<String, dynamic> json) => _$GeneratedVersionFromJson(json);
}
