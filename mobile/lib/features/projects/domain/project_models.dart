import 'package:freezed_annotation/freezed_annotation.dart';

part 'project_models.freezed.dart';
part 'project_models.g.dart';

@freezed
class ProjectCampaign with _$ProjectCampaign {
  const factory ProjectCampaign({
    required String id,
    required String workspaceId,
    required String brandId,
    required String productServiceId,
    required String name,
    required String description,
    required String campaignObjective,
    required String targetPlatform,
    required String campaignType,
    @Default('ACTIVE') String status,
    DateTime? createdAt,
  }) = _ProjectCampaign;

  factory ProjectCampaign.fromJson(Map<String, dynamic> json) => _$ProjectCampaignFromJson(json);
}

@freezed
class CreateProjectRequest with _$CreateProjectRequest {
  const factory CreateProjectRequest({
    required String productServiceId,
    required String name,
    required String description,
    required String campaignObjective,
    required String targetPlatform,
    required String campaignType,
  }) = _CreateProjectRequest;

  factory CreateProjectRequest.fromJson(Map<String, dynamic> json) => _$CreateProjectRequestFromJson(json);
}
