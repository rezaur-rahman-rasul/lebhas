// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'project_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

ProjectCampaign _$ProjectCampaignFromJson(Map<String, dynamic> json) {
  return _ProjectCampaign.fromJson(json);
}

/// @nodoc
mixin _$ProjectCampaign {
  String get id => throw _privateConstructorUsedError;
  String get workspaceId => throw _privateConstructorUsedError;
  String get brandId => throw _privateConstructorUsedError;
  String get productServiceId => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get description => throw _privateConstructorUsedError;
  String get campaignObjective => throw _privateConstructorUsedError;
  String get targetPlatform => throw _privateConstructorUsedError;
  String get campaignType => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;
  DateTime? get createdAt => throw _privateConstructorUsedError;

  /// Serializes this ProjectCampaign to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of ProjectCampaign
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $ProjectCampaignCopyWith<ProjectCampaign> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ProjectCampaignCopyWith<$Res> {
  factory $ProjectCampaignCopyWith(
          ProjectCampaign value, $Res Function(ProjectCampaign) then) =
      _$ProjectCampaignCopyWithImpl<$Res, ProjectCampaign>;
  @useResult
  $Res call(
      {String id,
      String workspaceId,
      String brandId,
      String productServiceId,
      String name,
      String description,
      String campaignObjective,
      String targetPlatform,
      String campaignType,
      String status,
      DateTime? createdAt});
}

/// @nodoc
class _$ProjectCampaignCopyWithImpl<$Res, $Val extends ProjectCampaign>
    implements $ProjectCampaignCopyWith<$Res> {
  _$ProjectCampaignCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of ProjectCampaign
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? workspaceId = null,
    Object? brandId = null,
    Object? productServiceId = null,
    Object? name = null,
    Object? description = null,
    Object? campaignObjective = null,
    Object? targetPlatform = null,
    Object? campaignType = null,
    Object? status = null,
    Object? createdAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      workspaceId: null == workspaceId
          ? _value.workspaceId
          : workspaceId // ignore: cast_nullable_to_non_nullable
              as String,
      brandId: null == brandId
          ? _value.brandId
          : brandId // ignore: cast_nullable_to_non_nullable
              as String,
      productServiceId: null == productServiceId
          ? _value.productServiceId
          : productServiceId // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      campaignObjective: null == campaignObjective
          ? _value.campaignObjective
          : campaignObjective // ignore: cast_nullable_to_non_nullable
              as String,
      targetPlatform: null == targetPlatform
          ? _value.targetPlatform
          : targetPlatform // ignore: cast_nullable_to_non_nullable
              as String,
      campaignType: null == campaignType
          ? _value.campaignType
          : campaignType // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      createdAt: freezed == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$ProjectCampaignImplCopyWith<$Res>
    implements $ProjectCampaignCopyWith<$Res> {
  factory _$$ProjectCampaignImplCopyWith(_$ProjectCampaignImpl value,
          $Res Function(_$ProjectCampaignImpl) then) =
      __$$ProjectCampaignImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String workspaceId,
      String brandId,
      String productServiceId,
      String name,
      String description,
      String campaignObjective,
      String targetPlatform,
      String campaignType,
      String status,
      DateTime? createdAt});
}

/// @nodoc
class __$$ProjectCampaignImplCopyWithImpl<$Res>
    extends _$ProjectCampaignCopyWithImpl<$Res, _$ProjectCampaignImpl>
    implements _$$ProjectCampaignImplCopyWith<$Res> {
  __$$ProjectCampaignImplCopyWithImpl(
      _$ProjectCampaignImpl _value, $Res Function(_$ProjectCampaignImpl) _then)
      : super(_value, _then);

  /// Create a copy of ProjectCampaign
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? workspaceId = null,
    Object? brandId = null,
    Object? productServiceId = null,
    Object? name = null,
    Object? description = null,
    Object? campaignObjective = null,
    Object? targetPlatform = null,
    Object? campaignType = null,
    Object? status = null,
    Object? createdAt = freezed,
  }) {
    return _then(_$ProjectCampaignImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      workspaceId: null == workspaceId
          ? _value.workspaceId
          : workspaceId // ignore: cast_nullable_to_non_nullable
              as String,
      brandId: null == brandId
          ? _value.brandId
          : brandId // ignore: cast_nullable_to_non_nullable
              as String,
      productServiceId: null == productServiceId
          ? _value.productServiceId
          : productServiceId // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      campaignObjective: null == campaignObjective
          ? _value.campaignObjective
          : campaignObjective // ignore: cast_nullable_to_non_nullable
              as String,
      targetPlatform: null == targetPlatform
          ? _value.targetPlatform
          : targetPlatform // ignore: cast_nullable_to_non_nullable
              as String,
      campaignType: null == campaignType
          ? _value.campaignType
          : campaignType // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      createdAt: freezed == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$ProjectCampaignImpl implements _ProjectCampaign {
  const _$ProjectCampaignImpl(
      {required this.id,
      required this.workspaceId,
      required this.brandId,
      required this.productServiceId,
      required this.name,
      required this.description,
      required this.campaignObjective,
      required this.targetPlatform,
      required this.campaignType,
      this.status = 'ACTIVE',
      this.createdAt});

  factory _$ProjectCampaignImpl.fromJson(Map<String, dynamic> json) =>
      _$$ProjectCampaignImplFromJson(json);

  @override
  final String id;
  @override
  final String workspaceId;
  @override
  final String brandId;
  @override
  final String productServiceId;
  @override
  final String name;
  @override
  final String description;
  @override
  final String campaignObjective;
  @override
  final String targetPlatform;
  @override
  final String campaignType;
  @override
  @JsonKey()
  final String status;
  @override
  final DateTime? createdAt;

  @override
  String toString() {
    return 'ProjectCampaign(id: $id, workspaceId: $workspaceId, brandId: $brandId, productServiceId: $productServiceId, name: $name, description: $description, campaignObjective: $campaignObjective, targetPlatform: $targetPlatform, campaignType: $campaignType, status: $status, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ProjectCampaignImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.workspaceId, workspaceId) ||
                other.workspaceId == workspaceId) &&
            (identical(other.brandId, brandId) || other.brandId == brandId) &&
            (identical(other.productServiceId, productServiceId) ||
                other.productServiceId == productServiceId) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.campaignObjective, campaignObjective) ||
                other.campaignObjective == campaignObjective) &&
            (identical(other.targetPlatform, targetPlatform) ||
                other.targetPlatform == targetPlatform) &&
            (identical(other.campaignType, campaignType) ||
                other.campaignType == campaignType) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      workspaceId,
      brandId,
      productServiceId,
      name,
      description,
      campaignObjective,
      targetPlatform,
      campaignType,
      status,
      createdAt);

  /// Create a copy of ProjectCampaign
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$ProjectCampaignImplCopyWith<_$ProjectCampaignImpl> get copyWith =>
      __$$ProjectCampaignImplCopyWithImpl<_$ProjectCampaignImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$ProjectCampaignImplToJson(
      this,
    );
  }
}

abstract class _ProjectCampaign implements ProjectCampaign {
  const factory _ProjectCampaign(
      {required final String id,
      required final String workspaceId,
      required final String brandId,
      required final String productServiceId,
      required final String name,
      required final String description,
      required final String campaignObjective,
      required final String targetPlatform,
      required final String campaignType,
      final String status,
      final DateTime? createdAt}) = _$ProjectCampaignImpl;

  factory _ProjectCampaign.fromJson(Map<String, dynamic> json) =
      _$ProjectCampaignImpl.fromJson;

  @override
  String get id;
  @override
  String get workspaceId;
  @override
  String get brandId;
  @override
  String get productServiceId;
  @override
  String get name;
  @override
  String get description;
  @override
  String get campaignObjective;
  @override
  String get targetPlatform;
  @override
  String get campaignType;
  @override
  String get status;
  @override
  DateTime? get createdAt;

  /// Create a copy of ProjectCampaign
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$ProjectCampaignImplCopyWith<_$ProjectCampaignImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

CreateProjectRequest _$CreateProjectRequestFromJson(Map<String, dynamic> json) {
  return _CreateProjectRequest.fromJson(json);
}

/// @nodoc
mixin _$CreateProjectRequest {
  String get productServiceId => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get description => throw _privateConstructorUsedError;
  String get campaignObjective => throw _privateConstructorUsedError;
  String get targetPlatform => throw _privateConstructorUsedError;
  String get campaignType => throw _privateConstructorUsedError;

  /// Serializes this CreateProjectRequest to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of CreateProjectRequest
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $CreateProjectRequestCopyWith<CreateProjectRequest> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $CreateProjectRequestCopyWith<$Res> {
  factory $CreateProjectRequestCopyWith(CreateProjectRequest value,
          $Res Function(CreateProjectRequest) then) =
      _$CreateProjectRequestCopyWithImpl<$Res, CreateProjectRequest>;
  @useResult
  $Res call(
      {String productServiceId,
      String name,
      String description,
      String campaignObjective,
      String targetPlatform,
      String campaignType});
}

/// @nodoc
class _$CreateProjectRequestCopyWithImpl<$Res,
        $Val extends CreateProjectRequest>
    implements $CreateProjectRequestCopyWith<$Res> {
  _$CreateProjectRequestCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of CreateProjectRequest
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? productServiceId = null,
    Object? name = null,
    Object? description = null,
    Object? campaignObjective = null,
    Object? targetPlatform = null,
    Object? campaignType = null,
  }) {
    return _then(_value.copyWith(
      productServiceId: null == productServiceId
          ? _value.productServiceId
          : productServiceId // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      campaignObjective: null == campaignObjective
          ? _value.campaignObjective
          : campaignObjective // ignore: cast_nullable_to_non_nullable
              as String,
      targetPlatform: null == targetPlatform
          ? _value.targetPlatform
          : targetPlatform // ignore: cast_nullable_to_non_nullable
              as String,
      campaignType: null == campaignType
          ? _value.campaignType
          : campaignType // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$CreateProjectRequestImplCopyWith<$Res>
    implements $CreateProjectRequestCopyWith<$Res> {
  factory _$$CreateProjectRequestImplCopyWith(_$CreateProjectRequestImpl value,
          $Res Function(_$CreateProjectRequestImpl) then) =
      __$$CreateProjectRequestImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String productServiceId,
      String name,
      String description,
      String campaignObjective,
      String targetPlatform,
      String campaignType});
}

/// @nodoc
class __$$CreateProjectRequestImplCopyWithImpl<$Res>
    extends _$CreateProjectRequestCopyWithImpl<$Res, _$CreateProjectRequestImpl>
    implements _$$CreateProjectRequestImplCopyWith<$Res> {
  __$$CreateProjectRequestImplCopyWithImpl(_$CreateProjectRequestImpl _value,
      $Res Function(_$CreateProjectRequestImpl) _then)
      : super(_value, _then);

  /// Create a copy of CreateProjectRequest
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? productServiceId = null,
    Object? name = null,
    Object? description = null,
    Object? campaignObjective = null,
    Object? targetPlatform = null,
    Object? campaignType = null,
  }) {
    return _then(_$CreateProjectRequestImpl(
      productServiceId: null == productServiceId
          ? _value.productServiceId
          : productServiceId // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      campaignObjective: null == campaignObjective
          ? _value.campaignObjective
          : campaignObjective // ignore: cast_nullable_to_non_nullable
              as String,
      targetPlatform: null == targetPlatform
          ? _value.targetPlatform
          : targetPlatform // ignore: cast_nullable_to_non_nullable
              as String,
      campaignType: null == campaignType
          ? _value.campaignType
          : campaignType // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$CreateProjectRequestImpl implements _CreateProjectRequest {
  const _$CreateProjectRequestImpl(
      {required this.productServiceId,
      required this.name,
      required this.description,
      required this.campaignObjective,
      required this.targetPlatform,
      required this.campaignType});

  factory _$CreateProjectRequestImpl.fromJson(Map<String, dynamic> json) =>
      _$$CreateProjectRequestImplFromJson(json);

  @override
  final String productServiceId;
  @override
  final String name;
  @override
  final String description;
  @override
  final String campaignObjective;
  @override
  final String targetPlatform;
  @override
  final String campaignType;

  @override
  String toString() {
    return 'CreateProjectRequest(productServiceId: $productServiceId, name: $name, description: $description, campaignObjective: $campaignObjective, targetPlatform: $targetPlatform, campaignType: $campaignType)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$CreateProjectRequestImpl &&
            (identical(other.productServiceId, productServiceId) ||
                other.productServiceId == productServiceId) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.campaignObjective, campaignObjective) ||
                other.campaignObjective == campaignObjective) &&
            (identical(other.targetPlatform, targetPlatform) ||
                other.targetPlatform == targetPlatform) &&
            (identical(other.campaignType, campaignType) ||
                other.campaignType == campaignType));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, productServiceId, name,
      description, campaignObjective, targetPlatform, campaignType);

  /// Create a copy of CreateProjectRequest
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$CreateProjectRequestImplCopyWith<_$CreateProjectRequestImpl>
      get copyWith =>
          __$$CreateProjectRequestImplCopyWithImpl<_$CreateProjectRequestImpl>(
              this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$CreateProjectRequestImplToJson(
      this,
    );
  }
}

abstract class _CreateProjectRequest implements CreateProjectRequest {
  const factory _CreateProjectRequest(
      {required final String productServiceId,
      required final String name,
      required final String description,
      required final String campaignObjective,
      required final String targetPlatform,
      required final String campaignType}) = _$CreateProjectRequestImpl;

  factory _CreateProjectRequest.fromJson(Map<String, dynamic> json) =
      _$CreateProjectRequestImpl.fromJson;

  @override
  String get productServiceId;
  @override
  String get name;
  @override
  String get description;
  @override
  String get campaignObjective;
  @override
  String get targetPlatform;
  @override
  String get campaignType;

  /// Create a copy of CreateProjectRequest
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$CreateProjectRequestImplCopyWith<_$CreateProjectRequestImpl>
      get copyWith => throw _privateConstructorUsedError;
}
