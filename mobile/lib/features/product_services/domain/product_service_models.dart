import 'package:freezed_annotation/freezed_annotation.dart';

part 'product_service_models.freezed.dart';
part 'product_service_models.g.dart';

@freezed
class ProductService with _$ProductService {
  const factory ProductService({
    required String id,
    required String workspaceId,
    required String brandId,
    required String name,
    required String description,
    required String category,
    required String targetAudience,
    required String sellingPoints,
    @Default('ACTIVE') String status,
    DateTime? createdAt,
  }) = _ProductService;

  factory ProductService.fromJson(Map<String, dynamic> json) => _$ProductServiceFromJson(json);
}

@freezed
class CreateProductServiceRequest with _$CreateProductServiceRequest {
  const factory CreateProductServiceRequest({
    required String brandId,
    required String name,
    required String description,
    required String category,
    required String targetAudience,
    required String sellingPoints,
  }) = _CreateProductServiceRequest;

  factory CreateProductServiceRequest.fromJson(Map<String, dynamic> json) => _$CreateProductServiceRequestFromJson(json);
}
