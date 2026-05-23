// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'brand_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Brand _$BrandFromJson(Map<String, dynamic> json) {
  return _Brand.fromJson(json);
}

/// @nodoc
mixin _$Brand {
  String get id => throw _privateConstructorUsedError;
  String get workspaceId => throw _privateConstructorUsedError;
  String get ownerUserId => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get businessType => throw _privateConstructorUsedError;
  String get industry => throw _privateConstructorUsedError;
  String get targetAudience => throw _privateConstructorUsedError;
  String get brandVoice => throw _privateConstructorUsedError;
  String get preferredCTA => throw _privateConstructorUsedError;
  String? get primaryColor => throw _privateConstructorUsedError;
  String? get secondaryColor => throw _privateConstructorUsedError;
  String? get website => throw _privateConstructorUsedError;
  String? get facebookUrl => throw _privateConstructorUsedError;
  String? get instagramUrl => throw _privateConstructorUsedError;
  String? get linkedinUrl => throw _privateConstructorUsedError;
  String? get tiktokUrl => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;
  DateTime? get createdAt => throw _privateConstructorUsedError;

  /// Serializes this Brand to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Brand
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $BrandCopyWith<Brand> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $BrandCopyWith<$Res> {
  factory $BrandCopyWith(Brand value, $Res Function(Brand) then) =
      _$BrandCopyWithImpl<$Res, Brand>;
  @useResult
  $Res call(
      {String id,
      String workspaceId,
      String ownerUserId,
      String name,
      String businessType,
      String industry,
      String targetAudience,
      String brandVoice,
      String preferredCTA,
      String? primaryColor,
      String? secondaryColor,
      String? website,
      String? facebookUrl,
      String? instagramUrl,
      String? linkedinUrl,
      String? tiktokUrl,
      String status,
      DateTime? createdAt});
}

/// @nodoc
class _$BrandCopyWithImpl<$Res, $Val extends Brand>
    implements $BrandCopyWith<$Res> {
  _$BrandCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Brand
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? workspaceId = null,
    Object? ownerUserId = null,
    Object? name = null,
    Object? businessType = null,
    Object? industry = null,
    Object? targetAudience = null,
    Object? brandVoice = null,
    Object? preferredCTA = null,
    Object? primaryColor = freezed,
    Object? secondaryColor = freezed,
    Object? website = freezed,
    Object? facebookUrl = freezed,
    Object? instagramUrl = freezed,
    Object? linkedinUrl = freezed,
    Object? tiktokUrl = freezed,
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
      ownerUserId: null == ownerUserId
          ? _value.ownerUserId
          : ownerUserId // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      businessType: null == businessType
          ? _value.businessType
          : businessType // ignore: cast_nullable_to_non_nullable
              as String,
      industry: null == industry
          ? _value.industry
          : industry // ignore: cast_nullable_to_non_nullable
              as String,
      targetAudience: null == targetAudience
          ? _value.targetAudience
          : targetAudience // ignore: cast_nullable_to_non_nullable
              as String,
      brandVoice: null == brandVoice
          ? _value.brandVoice
          : brandVoice // ignore: cast_nullable_to_non_nullable
              as String,
      preferredCTA: null == preferredCTA
          ? _value.preferredCTA
          : preferredCTA // ignore: cast_nullable_to_non_nullable
              as String,
      primaryColor: freezed == primaryColor
          ? _value.primaryColor
          : primaryColor // ignore: cast_nullable_to_non_nullable
              as String?,
      secondaryColor: freezed == secondaryColor
          ? _value.secondaryColor
          : secondaryColor // ignore: cast_nullable_to_non_nullable
              as String?,
      website: freezed == website
          ? _value.website
          : website // ignore: cast_nullable_to_non_nullable
              as String?,
      facebookUrl: freezed == facebookUrl
          ? _value.facebookUrl
          : facebookUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      instagramUrl: freezed == instagramUrl
          ? _value.instagramUrl
          : instagramUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      linkedinUrl: freezed == linkedinUrl
          ? _value.linkedinUrl
          : linkedinUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      tiktokUrl: freezed == tiktokUrl
          ? _value.tiktokUrl
          : tiktokUrl // ignore: cast_nullable_to_non_nullable
              as String?,
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
abstract class _$$BrandImplCopyWith<$Res> implements $BrandCopyWith<$Res> {
  factory _$$BrandImplCopyWith(
          _$BrandImpl value, $Res Function(_$BrandImpl) then) =
      __$$BrandImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String workspaceId,
      String ownerUserId,
      String name,
      String businessType,
      String industry,
      String targetAudience,
      String brandVoice,
      String preferredCTA,
      String? primaryColor,
      String? secondaryColor,
      String? website,
      String? facebookUrl,
      String? instagramUrl,
      String? linkedinUrl,
      String? tiktokUrl,
      String status,
      DateTime? createdAt});
}

/// @nodoc
class __$$BrandImplCopyWithImpl<$Res>
    extends _$BrandCopyWithImpl<$Res, _$BrandImpl>
    implements _$$BrandImplCopyWith<$Res> {
  __$$BrandImplCopyWithImpl(
      _$BrandImpl _value, $Res Function(_$BrandImpl) _then)
      : super(_value, _then);

  /// Create a copy of Brand
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? workspaceId = null,
    Object? ownerUserId = null,
    Object? name = null,
    Object? businessType = null,
    Object? industry = null,
    Object? targetAudience = null,
    Object? brandVoice = null,
    Object? preferredCTA = null,
    Object? primaryColor = freezed,
    Object? secondaryColor = freezed,
    Object? website = freezed,
    Object? facebookUrl = freezed,
    Object? instagramUrl = freezed,
    Object? linkedinUrl = freezed,
    Object? tiktokUrl = freezed,
    Object? status = null,
    Object? createdAt = freezed,
  }) {
    return _then(_$BrandImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      workspaceId: null == workspaceId
          ? _value.workspaceId
          : workspaceId // ignore: cast_nullable_to_non_nullable
              as String,
      ownerUserId: null == ownerUserId
          ? _value.ownerUserId
          : ownerUserId // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      businessType: null == businessType
          ? _value.businessType
          : businessType // ignore: cast_nullable_to_non_nullable
              as String,
      industry: null == industry
          ? _value.industry
          : industry // ignore: cast_nullable_to_non_nullable
              as String,
      targetAudience: null == targetAudience
          ? _value.targetAudience
          : targetAudience // ignore: cast_nullable_to_non_nullable
              as String,
      brandVoice: null == brandVoice
          ? _value.brandVoice
          : brandVoice // ignore: cast_nullable_to_non_nullable
              as String,
      preferredCTA: null == preferredCTA
          ? _value.preferredCTA
          : preferredCTA // ignore: cast_nullable_to_non_nullable
              as String,
      primaryColor: freezed == primaryColor
          ? _value.primaryColor
          : primaryColor // ignore: cast_nullable_to_non_nullable
              as String?,
      secondaryColor: freezed == secondaryColor
          ? _value.secondaryColor
          : secondaryColor // ignore: cast_nullable_to_non_nullable
              as String?,
      website: freezed == website
          ? _value.website
          : website // ignore: cast_nullable_to_non_nullable
              as String?,
      facebookUrl: freezed == facebookUrl
          ? _value.facebookUrl
          : facebookUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      instagramUrl: freezed == instagramUrl
          ? _value.instagramUrl
          : instagramUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      linkedinUrl: freezed == linkedinUrl
          ? _value.linkedinUrl
          : linkedinUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      tiktokUrl: freezed == tiktokUrl
          ? _value.tiktokUrl
          : tiktokUrl // ignore: cast_nullable_to_non_nullable
              as String?,
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
class _$BrandImpl implements _Brand {
  const _$BrandImpl(
      {required this.id,
      required this.workspaceId,
      required this.ownerUserId,
      required this.name,
      required this.businessType,
      required this.industry,
      required this.targetAudience,
      required this.brandVoice,
      required this.preferredCTA,
      this.primaryColor,
      this.secondaryColor,
      this.website,
      this.facebookUrl,
      this.instagramUrl,
      this.linkedinUrl,
      this.tiktokUrl,
      this.status = 'ACTIVE',
      this.createdAt});

  factory _$BrandImpl.fromJson(Map<String, dynamic> json) =>
      _$$BrandImplFromJson(json);

  @override
  final String id;
  @override
  final String workspaceId;
  @override
  final String ownerUserId;
  @override
  final String name;
  @override
  final String businessType;
  @override
  final String industry;
  @override
  final String targetAudience;
  @override
  final String brandVoice;
  @override
  final String preferredCTA;
  @override
  final String? primaryColor;
  @override
  final String? secondaryColor;
  @override
  final String? website;
  @override
  final String? facebookUrl;
  @override
  final String? instagramUrl;
  @override
  final String? linkedinUrl;
  @override
  final String? tiktokUrl;
  @override
  @JsonKey()
  final String status;
  @override
  final DateTime? createdAt;

  @override
  String toString() {
    return 'Brand(id: $id, workspaceId: $workspaceId, ownerUserId: $ownerUserId, name: $name, businessType: $businessType, industry: $industry, targetAudience: $targetAudience, brandVoice: $brandVoice, preferredCTA: $preferredCTA, primaryColor: $primaryColor, secondaryColor: $secondaryColor, website: $website, facebookUrl: $facebookUrl, instagramUrl: $instagramUrl, linkedinUrl: $linkedinUrl, tiktokUrl: $tiktokUrl, status: $status, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$BrandImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.workspaceId, workspaceId) ||
                other.workspaceId == workspaceId) &&
            (identical(other.ownerUserId, ownerUserId) ||
                other.ownerUserId == ownerUserId) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.businessType, businessType) ||
                other.businessType == businessType) &&
            (identical(other.industry, industry) ||
                other.industry == industry) &&
            (identical(other.targetAudience, targetAudience) ||
                other.targetAudience == targetAudience) &&
            (identical(other.brandVoice, brandVoice) ||
                other.brandVoice == brandVoice) &&
            (identical(other.preferredCTA, preferredCTA) ||
                other.preferredCTA == preferredCTA) &&
            (identical(other.primaryColor, primaryColor) ||
                other.primaryColor == primaryColor) &&
            (identical(other.secondaryColor, secondaryColor) ||
                other.secondaryColor == secondaryColor) &&
            (identical(other.website, website) || other.website == website) &&
            (identical(other.facebookUrl, facebookUrl) ||
                other.facebookUrl == facebookUrl) &&
            (identical(other.instagramUrl, instagramUrl) ||
                other.instagramUrl == instagramUrl) &&
            (identical(other.linkedinUrl, linkedinUrl) ||
                other.linkedinUrl == linkedinUrl) &&
            (identical(other.tiktokUrl, tiktokUrl) ||
                other.tiktokUrl == tiktokUrl) &&
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
      ownerUserId,
      name,
      businessType,
      industry,
      targetAudience,
      brandVoice,
      preferredCTA,
      primaryColor,
      secondaryColor,
      website,
      facebookUrl,
      instagramUrl,
      linkedinUrl,
      tiktokUrl,
      status,
      createdAt);

  /// Create a copy of Brand
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$BrandImplCopyWith<_$BrandImpl> get copyWith =>
      __$$BrandImplCopyWithImpl<_$BrandImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$BrandImplToJson(
      this,
    );
  }
}

abstract class _Brand implements Brand {
  const factory _Brand(
      {required final String id,
      required final String workspaceId,
      required final String ownerUserId,
      required final String name,
      required final String businessType,
      required final String industry,
      required final String targetAudience,
      required final String brandVoice,
      required final String preferredCTA,
      final String? primaryColor,
      final String? secondaryColor,
      final String? website,
      final String? facebookUrl,
      final String? instagramUrl,
      final String? linkedinUrl,
      final String? tiktokUrl,
      final String status,
      final DateTime? createdAt}) = _$BrandImpl;

  factory _Brand.fromJson(Map<String, dynamic> json) = _$BrandImpl.fromJson;

  @override
  String get id;
  @override
  String get workspaceId;
  @override
  String get ownerUserId;
  @override
  String get name;
  @override
  String get businessType;
  @override
  String get industry;
  @override
  String get targetAudience;
  @override
  String get brandVoice;
  @override
  String get preferredCTA;
  @override
  String? get primaryColor;
  @override
  String? get secondaryColor;
  @override
  String? get website;
  @override
  String? get facebookUrl;
  @override
  String? get instagramUrl;
  @override
  String? get linkedinUrl;
  @override
  String? get tiktokUrl;
  @override
  String get status;
  @override
  DateTime? get createdAt;

  /// Create a copy of Brand
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$BrandImplCopyWith<_$BrandImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

CreateBrandRequest _$CreateBrandRequestFromJson(Map<String, dynamic> json) {
  return _CreateBrandRequest.fromJson(json);
}

/// @nodoc
mixin _$CreateBrandRequest {
  String get name => throw _privateConstructorUsedError;
  String get businessType => throw _privateConstructorUsedError;
  String get industry => throw _privateConstructorUsedError;
  String get targetAudience => throw _privateConstructorUsedError;
  String get brandVoice => throw _privateConstructorUsedError;
  String get preferredCTA => throw _privateConstructorUsedError;
  String? get primaryColor => throw _privateConstructorUsedError;
  String? get secondaryColor => throw _privateConstructorUsedError;
  String? get website => throw _privateConstructorUsedError;
  String? get facebookUrl => throw _privateConstructorUsedError;
  String? get instagramUrl => throw _privateConstructorUsedError;
  String? get linkedinUrl => throw _privateConstructorUsedError;
  String? get tiktokUrl => throw _privateConstructorUsedError;

  /// Serializes this CreateBrandRequest to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of CreateBrandRequest
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $CreateBrandRequestCopyWith<CreateBrandRequest> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $CreateBrandRequestCopyWith<$Res> {
  factory $CreateBrandRequestCopyWith(
          CreateBrandRequest value, $Res Function(CreateBrandRequest) then) =
      _$CreateBrandRequestCopyWithImpl<$Res, CreateBrandRequest>;
  @useResult
  $Res call(
      {String name,
      String businessType,
      String industry,
      String targetAudience,
      String brandVoice,
      String preferredCTA,
      String? primaryColor,
      String? secondaryColor,
      String? website,
      String? facebookUrl,
      String? instagramUrl,
      String? linkedinUrl,
      String? tiktokUrl});
}

/// @nodoc
class _$CreateBrandRequestCopyWithImpl<$Res, $Val extends CreateBrandRequest>
    implements $CreateBrandRequestCopyWith<$Res> {
  _$CreateBrandRequestCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of CreateBrandRequest
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? name = null,
    Object? businessType = null,
    Object? industry = null,
    Object? targetAudience = null,
    Object? brandVoice = null,
    Object? preferredCTA = null,
    Object? primaryColor = freezed,
    Object? secondaryColor = freezed,
    Object? website = freezed,
    Object? facebookUrl = freezed,
    Object? instagramUrl = freezed,
    Object? linkedinUrl = freezed,
    Object? tiktokUrl = freezed,
  }) {
    return _then(_value.copyWith(
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      businessType: null == businessType
          ? _value.businessType
          : businessType // ignore: cast_nullable_to_non_nullable
              as String,
      industry: null == industry
          ? _value.industry
          : industry // ignore: cast_nullable_to_non_nullable
              as String,
      targetAudience: null == targetAudience
          ? _value.targetAudience
          : targetAudience // ignore: cast_nullable_to_non_nullable
              as String,
      brandVoice: null == brandVoice
          ? _value.brandVoice
          : brandVoice // ignore: cast_nullable_to_non_nullable
              as String,
      preferredCTA: null == preferredCTA
          ? _value.preferredCTA
          : preferredCTA // ignore: cast_nullable_to_non_nullable
              as String,
      primaryColor: freezed == primaryColor
          ? _value.primaryColor
          : primaryColor // ignore: cast_nullable_to_non_nullable
              as String?,
      secondaryColor: freezed == secondaryColor
          ? _value.secondaryColor
          : secondaryColor // ignore: cast_nullable_to_non_nullable
              as String?,
      website: freezed == website
          ? _value.website
          : website // ignore: cast_nullable_to_non_nullable
              as String?,
      facebookUrl: freezed == facebookUrl
          ? _value.facebookUrl
          : facebookUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      instagramUrl: freezed == instagramUrl
          ? _value.instagramUrl
          : instagramUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      linkedinUrl: freezed == linkedinUrl
          ? _value.linkedinUrl
          : linkedinUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      tiktokUrl: freezed == tiktokUrl
          ? _value.tiktokUrl
          : tiktokUrl // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$CreateBrandRequestImplCopyWith<$Res>
    implements $CreateBrandRequestCopyWith<$Res> {
  factory _$$CreateBrandRequestImplCopyWith(_$CreateBrandRequestImpl value,
          $Res Function(_$CreateBrandRequestImpl) then) =
      __$$CreateBrandRequestImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String name,
      String businessType,
      String industry,
      String targetAudience,
      String brandVoice,
      String preferredCTA,
      String? primaryColor,
      String? secondaryColor,
      String? website,
      String? facebookUrl,
      String? instagramUrl,
      String? linkedinUrl,
      String? tiktokUrl});
}

/// @nodoc
class __$$CreateBrandRequestImplCopyWithImpl<$Res>
    extends _$CreateBrandRequestCopyWithImpl<$Res, _$CreateBrandRequestImpl>
    implements _$$CreateBrandRequestImplCopyWith<$Res> {
  __$$CreateBrandRequestImplCopyWithImpl(_$CreateBrandRequestImpl _value,
      $Res Function(_$CreateBrandRequestImpl) _then)
      : super(_value, _then);

  /// Create a copy of CreateBrandRequest
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? name = null,
    Object? businessType = null,
    Object? industry = null,
    Object? targetAudience = null,
    Object? brandVoice = null,
    Object? preferredCTA = null,
    Object? primaryColor = freezed,
    Object? secondaryColor = freezed,
    Object? website = freezed,
    Object? facebookUrl = freezed,
    Object? instagramUrl = freezed,
    Object? linkedinUrl = freezed,
    Object? tiktokUrl = freezed,
  }) {
    return _then(_$CreateBrandRequestImpl(
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      businessType: null == businessType
          ? _value.businessType
          : businessType // ignore: cast_nullable_to_non_nullable
              as String,
      industry: null == industry
          ? _value.industry
          : industry // ignore: cast_nullable_to_non_nullable
              as String,
      targetAudience: null == targetAudience
          ? _value.targetAudience
          : targetAudience // ignore: cast_nullable_to_non_nullable
              as String,
      brandVoice: null == brandVoice
          ? _value.brandVoice
          : brandVoice // ignore: cast_nullable_to_non_nullable
              as String,
      preferredCTA: null == preferredCTA
          ? _value.preferredCTA
          : preferredCTA // ignore: cast_nullable_to_non_nullable
              as String,
      primaryColor: freezed == primaryColor
          ? _value.primaryColor
          : primaryColor // ignore: cast_nullable_to_non_nullable
              as String?,
      secondaryColor: freezed == secondaryColor
          ? _value.secondaryColor
          : secondaryColor // ignore: cast_nullable_to_non_nullable
              as String?,
      website: freezed == website
          ? _value.website
          : website // ignore: cast_nullable_to_non_nullable
              as String?,
      facebookUrl: freezed == facebookUrl
          ? _value.facebookUrl
          : facebookUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      instagramUrl: freezed == instagramUrl
          ? _value.instagramUrl
          : instagramUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      linkedinUrl: freezed == linkedinUrl
          ? _value.linkedinUrl
          : linkedinUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      tiktokUrl: freezed == tiktokUrl
          ? _value.tiktokUrl
          : tiktokUrl // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$CreateBrandRequestImpl implements _CreateBrandRequest {
  const _$CreateBrandRequestImpl(
      {required this.name,
      required this.businessType,
      required this.industry,
      required this.targetAudience,
      required this.brandVoice,
      required this.preferredCTA,
      this.primaryColor,
      this.secondaryColor,
      this.website,
      this.facebookUrl,
      this.instagramUrl,
      this.linkedinUrl,
      this.tiktokUrl});

  factory _$CreateBrandRequestImpl.fromJson(Map<String, dynamic> json) =>
      _$$CreateBrandRequestImplFromJson(json);

  @override
  final String name;
  @override
  final String businessType;
  @override
  final String industry;
  @override
  final String targetAudience;
  @override
  final String brandVoice;
  @override
  final String preferredCTA;
  @override
  final String? primaryColor;
  @override
  final String? secondaryColor;
  @override
  final String? website;
  @override
  final String? facebookUrl;
  @override
  final String? instagramUrl;
  @override
  final String? linkedinUrl;
  @override
  final String? tiktokUrl;

  @override
  String toString() {
    return 'CreateBrandRequest(name: $name, businessType: $businessType, industry: $industry, targetAudience: $targetAudience, brandVoice: $brandVoice, preferredCTA: $preferredCTA, primaryColor: $primaryColor, secondaryColor: $secondaryColor, website: $website, facebookUrl: $facebookUrl, instagramUrl: $instagramUrl, linkedinUrl: $linkedinUrl, tiktokUrl: $tiktokUrl)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$CreateBrandRequestImpl &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.businessType, businessType) ||
                other.businessType == businessType) &&
            (identical(other.industry, industry) ||
                other.industry == industry) &&
            (identical(other.targetAudience, targetAudience) ||
                other.targetAudience == targetAudience) &&
            (identical(other.brandVoice, brandVoice) ||
                other.brandVoice == brandVoice) &&
            (identical(other.preferredCTA, preferredCTA) ||
                other.preferredCTA == preferredCTA) &&
            (identical(other.primaryColor, primaryColor) ||
                other.primaryColor == primaryColor) &&
            (identical(other.secondaryColor, secondaryColor) ||
                other.secondaryColor == secondaryColor) &&
            (identical(other.website, website) || other.website == website) &&
            (identical(other.facebookUrl, facebookUrl) ||
                other.facebookUrl == facebookUrl) &&
            (identical(other.instagramUrl, instagramUrl) ||
                other.instagramUrl == instagramUrl) &&
            (identical(other.linkedinUrl, linkedinUrl) ||
                other.linkedinUrl == linkedinUrl) &&
            (identical(other.tiktokUrl, tiktokUrl) ||
                other.tiktokUrl == tiktokUrl));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      name,
      businessType,
      industry,
      targetAudience,
      brandVoice,
      preferredCTA,
      primaryColor,
      secondaryColor,
      website,
      facebookUrl,
      instagramUrl,
      linkedinUrl,
      tiktokUrl);

  /// Create a copy of CreateBrandRequest
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$CreateBrandRequestImplCopyWith<_$CreateBrandRequestImpl> get copyWith =>
      __$$CreateBrandRequestImplCopyWithImpl<_$CreateBrandRequestImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$CreateBrandRequestImplToJson(
      this,
    );
  }
}

abstract class _CreateBrandRequest implements CreateBrandRequest {
  const factory _CreateBrandRequest(
      {required final String name,
      required final String businessType,
      required final String industry,
      required final String targetAudience,
      required final String brandVoice,
      required final String preferredCTA,
      final String? primaryColor,
      final String? secondaryColor,
      final String? website,
      final String? facebookUrl,
      final String? instagramUrl,
      final String? linkedinUrl,
      final String? tiktokUrl}) = _$CreateBrandRequestImpl;

  factory _CreateBrandRequest.fromJson(Map<String, dynamic> json) =
      _$CreateBrandRequestImpl.fromJson;

  @override
  String get name;
  @override
  String get businessType;
  @override
  String get industry;
  @override
  String get targetAudience;
  @override
  String get brandVoice;
  @override
  String get preferredCTA;
  @override
  String? get primaryColor;
  @override
  String? get secondaryColor;
  @override
  String? get website;
  @override
  String? get facebookUrl;
  @override
  String? get instagramUrl;
  @override
  String? get linkedinUrl;
  @override
  String? get tiktokUrl;

  /// Create a copy of CreateBrandRequest
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$CreateBrandRequestImplCopyWith<_$CreateBrandRequestImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
