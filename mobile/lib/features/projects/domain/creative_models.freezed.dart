// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'creative_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

CreativeRequest _$CreativeRequestFromJson(Map<String, dynamic> json) {
  return _CreativeRequest.fromJson(json);
}

/// @nodoc
mixin _$CreativeRequest {
  String get id => throw _privateConstructorUsedError;
  String get projectId => throw _privateConstructorUsedError;
  String get title => throw _privateConstructorUsedError;
  String get prompt => throw _privateConstructorUsedError;
  String? get negativePrompt => throw _privateConstructorUsedError;
  String get aspectRatio => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;
  DateTime? get createdAt => throw _privateConstructorUsedError;

  /// Serializes this CreativeRequest to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of CreativeRequest
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $CreativeRequestCopyWith<CreativeRequest> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $CreativeRequestCopyWith<$Res> {
  factory $CreativeRequestCopyWith(
          CreativeRequest value, $Res Function(CreativeRequest) then) =
      _$CreativeRequestCopyWithImpl<$Res, CreativeRequest>;
  @useResult
  $Res call(
      {String id,
      String projectId,
      String title,
      String prompt,
      String? negativePrompt,
      String aspectRatio,
      String status,
      DateTime? createdAt});
}

/// @nodoc
class _$CreativeRequestCopyWithImpl<$Res, $Val extends CreativeRequest>
    implements $CreativeRequestCopyWith<$Res> {
  _$CreativeRequestCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of CreativeRequest
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? projectId = null,
    Object? title = null,
    Object? prompt = null,
    Object? negativePrompt = freezed,
    Object? aspectRatio = null,
    Object? status = null,
    Object? createdAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      projectId: null == projectId
          ? _value.projectId
          : projectId // ignore: cast_nullable_to_non_nullable
              as String,
      title: null == title
          ? _value.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      prompt: null == prompt
          ? _value.prompt
          : prompt // ignore: cast_nullable_to_non_nullable
              as String,
      negativePrompt: freezed == negativePrompt
          ? _value.negativePrompt
          : negativePrompt // ignore: cast_nullable_to_non_nullable
              as String?,
      aspectRatio: null == aspectRatio
          ? _value.aspectRatio
          : aspectRatio // ignore: cast_nullable_to_non_nullable
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
abstract class _$$CreativeRequestImplCopyWith<$Res>
    implements $CreativeRequestCopyWith<$Res> {
  factory _$$CreativeRequestImplCopyWith(_$CreativeRequestImpl value,
          $Res Function(_$CreativeRequestImpl) then) =
      __$$CreativeRequestImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String projectId,
      String title,
      String prompt,
      String? negativePrompt,
      String aspectRatio,
      String status,
      DateTime? createdAt});
}

/// @nodoc
class __$$CreativeRequestImplCopyWithImpl<$Res>
    extends _$CreativeRequestCopyWithImpl<$Res, _$CreativeRequestImpl>
    implements _$$CreativeRequestImplCopyWith<$Res> {
  __$$CreativeRequestImplCopyWithImpl(
      _$CreativeRequestImpl _value, $Res Function(_$CreativeRequestImpl) _then)
      : super(_value, _then);

  /// Create a copy of CreativeRequest
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? projectId = null,
    Object? title = null,
    Object? prompt = null,
    Object? negativePrompt = freezed,
    Object? aspectRatio = null,
    Object? status = null,
    Object? createdAt = freezed,
  }) {
    return _then(_$CreativeRequestImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      projectId: null == projectId
          ? _value.projectId
          : projectId // ignore: cast_nullable_to_non_nullable
              as String,
      title: null == title
          ? _value.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      prompt: null == prompt
          ? _value.prompt
          : prompt // ignore: cast_nullable_to_non_nullable
              as String,
      negativePrompt: freezed == negativePrompt
          ? _value.negativePrompt
          : negativePrompt // ignore: cast_nullable_to_non_nullable
              as String?,
      aspectRatio: null == aspectRatio
          ? _value.aspectRatio
          : aspectRatio // ignore: cast_nullable_to_non_nullable
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
class _$CreativeRequestImpl implements _CreativeRequest {
  const _$CreativeRequestImpl(
      {required this.id,
      required this.projectId,
      required this.title,
      required this.prompt,
      this.negativePrompt,
      required this.aspectRatio,
      this.status = 'PENDING',
      this.createdAt});

  factory _$CreativeRequestImpl.fromJson(Map<String, dynamic> json) =>
      _$$CreativeRequestImplFromJson(json);

  @override
  final String id;
  @override
  final String projectId;
  @override
  final String title;
  @override
  final String prompt;
  @override
  final String? negativePrompt;
  @override
  final String aspectRatio;
  @override
  @JsonKey()
  final String status;
  @override
  final DateTime? createdAt;

  @override
  String toString() {
    return 'CreativeRequest(id: $id, projectId: $projectId, title: $title, prompt: $prompt, negativePrompt: $negativePrompt, aspectRatio: $aspectRatio, status: $status, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$CreativeRequestImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.projectId, projectId) ||
                other.projectId == projectId) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.prompt, prompt) || other.prompt == prompt) &&
            (identical(other.negativePrompt, negativePrompt) ||
                other.negativePrompt == negativePrompt) &&
            (identical(other.aspectRatio, aspectRatio) ||
                other.aspectRatio == aspectRatio) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, projectId, title, prompt,
      negativePrompt, aspectRatio, status, createdAt);

  /// Create a copy of CreativeRequest
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$CreativeRequestImplCopyWith<_$CreativeRequestImpl> get copyWith =>
      __$$CreativeRequestImplCopyWithImpl<_$CreativeRequestImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$CreativeRequestImplToJson(
      this,
    );
  }
}

abstract class _CreativeRequest implements CreativeRequest {
  const factory _CreativeRequest(
      {required final String id,
      required final String projectId,
      required final String title,
      required final String prompt,
      final String? negativePrompt,
      required final String aspectRatio,
      final String status,
      final DateTime? createdAt}) = _$CreativeRequestImpl;

  factory _CreativeRequest.fromJson(Map<String, dynamic> json) =
      _$CreativeRequestImpl.fromJson;

  @override
  String get id;
  @override
  String get projectId;
  @override
  String get title;
  @override
  String get prompt;
  @override
  String? get negativePrompt;
  @override
  String get aspectRatio;
  @override
  String get status;
  @override
  DateTime? get createdAt;

  /// Create a copy of CreativeRequest
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$CreativeRequestImplCopyWith<_$CreativeRequestImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

GeneratedVersion _$GeneratedVersionFromJson(Map<String, dynamic> json) {
  return _GeneratedVersion.fromJson(json);
}

/// @nodoc
mixin _$GeneratedVersion {
  String get id => throw _privateConstructorUsedError;
  String get requestId => throw _privateConstructorUsedError;
  String get imageUrl => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;
  bool get isApproved => throw _privateConstructorUsedError;
  double? get score => throw _privateConstructorUsedError;
  DateTime? get createdAt => throw _privateConstructorUsedError;

  /// Serializes this GeneratedVersion to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of GeneratedVersion
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $GeneratedVersionCopyWith<GeneratedVersion> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $GeneratedVersionCopyWith<$Res> {
  factory $GeneratedVersionCopyWith(
          GeneratedVersion value, $Res Function(GeneratedVersion) then) =
      _$GeneratedVersionCopyWithImpl<$Res, GeneratedVersion>;
  @useResult
  $Res call(
      {String id,
      String requestId,
      String imageUrl,
      String status,
      bool isApproved,
      double? score,
      DateTime? createdAt});
}

/// @nodoc
class _$GeneratedVersionCopyWithImpl<$Res, $Val extends GeneratedVersion>
    implements $GeneratedVersionCopyWith<$Res> {
  _$GeneratedVersionCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of GeneratedVersion
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? requestId = null,
    Object? imageUrl = null,
    Object? status = null,
    Object? isApproved = null,
    Object? score = freezed,
    Object? createdAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      requestId: null == requestId
          ? _value.requestId
          : requestId // ignore: cast_nullable_to_non_nullable
              as String,
      imageUrl: null == imageUrl
          ? _value.imageUrl
          : imageUrl // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      isApproved: null == isApproved
          ? _value.isApproved
          : isApproved // ignore: cast_nullable_to_non_nullable
              as bool,
      score: freezed == score
          ? _value.score
          : score // ignore: cast_nullable_to_non_nullable
              as double?,
      createdAt: freezed == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$GeneratedVersionImplCopyWith<$Res>
    implements $GeneratedVersionCopyWith<$Res> {
  factory _$$GeneratedVersionImplCopyWith(_$GeneratedVersionImpl value,
          $Res Function(_$GeneratedVersionImpl) then) =
      __$$GeneratedVersionImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String requestId,
      String imageUrl,
      String status,
      bool isApproved,
      double? score,
      DateTime? createdAt});
}

/// @nodoc
class __$$GeneratedVersionImplCopyWithImpl<$Res>
    extends _$GeneratedVersionCopyWithImpl<$Res, _$GeneratedVersionImpl>
    implements _$$GeneratedVersionImplCopyWith<$Res> {
  __$$GeneratedVersionImplCopyWithImpl(_$GeneratedVersionImpl _value,
      $Res Function(_$GeneratedVersionImpl) _then)
      : super(_value, _then);

  /// Create a copy of GeneratedVersion
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? requestId = null,
    Object? imageUrl = null,
    Object? status = null,
    Object? isApproved = null,
    Object? score = freezed,
    Object? createdAt = freezed,
  }) {
    return _then(_$GeneratedVersionImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      requestId: null == requestId
          ? _value.requestId
          : requestId // ignore: cast_nullable_to_non_nullable
              as String,
      imageUrl: null == imageUrl
          ? _value.imageUrl
          : imageUrl // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      isApproved: null == isApproved
          ? _value.isApproved
          : isApproved // ignore: cast_nullable_to_non_nullable
              as bool,
      score: freezed == score
          ? _value.score
          : score // ignore: cast_nullable_to_non_nullable
              as double?,
      createdAt: freezed == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$GeneratedVersionImpl implements _GeneratedVersion {
  const _$GeneratedVersionImpl(
      {required this.id,
      required this.requestId,
      required this.imageUrl,
      this.status = 'DRAFT',
      this.isApproved = false,
      this.score,
      this.createdAt});

  factory _$GeneratedVersionImpl.fromJson(Map<String, dynamic> json) =>
      _$$GeneratedVersionImplFromJson(json);

  @override
  final String id;
  @override
  final String requestId;
  @override
  final String imageUrl;
  @override
  @JsonKey()
  final String status;
  @override
  @JsonKey()
  final bool isApproved;
  @override
  final double? score;
  @override
  final DateTime? createdAt;

  @override
  String toString() {
    return 'GeneratedVersion(id: $id, requestId: $requestId, imageUrl: $imageUrl, status: $status, isApproved: $isApproved, score: $score, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$GeneratedVersionImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.requestId, requestId) ||
                other.requestId == requestId) &&
            (identical(other.imageUrl, imageUrl) ||
                other.imageUrl == imageUrl) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.isApproved, isApproved) ||
                other.isApproved == isApproved) &&
            (identical(other.score, score) || other.score == score) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, requestId, imageUrl, status,
      isApproved, score, createdAt);

  /// Create a copy of GeneratedVersion
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$GeneratedVersionImplCopyWith<_$GeneratedVersionImpl> get copyWith =>
      __$$GeneratedVersionImplCopyWithImpl<_$GeneratedVersionImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$GeneratedVersionImplToJson(
      this,
    );
  }
}

abstract class _GeneratedVersion implements GeneratedVersion {
  const factory _GeneratedVersion(
      {required final String id,
      required final String requestId,
      required final String imageUrl,
      final String status,
      final bool isApproved,
      final double? score,
      final DateTime? createdAt}) = _$GeneratedVersionImpl;

  factory _GeneratedVersion.fromJson(Map<String, dynamic> json) =
      _$GeneratedVersionImpl.fromJson;

  @override
  String get id;
  @override
  String get requestId;
  @override
  String get imageUrl;
  @override
  String get status;
  @override
  bool get isApproved;
  @override
  double? get score;
  @override
  DateTime? get createdAt;

  /// Create a copy of GeneratedVersion
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$GeneratedVersionImplCopyWith<_$GeneratedVersionImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
