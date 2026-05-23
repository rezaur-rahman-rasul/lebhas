// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'asset_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

StorageFile _$StorageFileFromJson(Map<String, dynamic> json) {
  return _StorageFile.fromJson(json);
}

/// @nodoc
mixin _$StorageFile {
  String get id => throw _privateConstructorUsedError;
  String get workspaceId => throw _privateConstructorUsedError;
  String get provider => throw _privateConstructorUsedError;
  String get bucket => throw _privateConstructorUsedError;
  String get objectKey => throw _privateConstructorUsedError;
  String? get cdnUrl => throw _privateConstructorUsedError;
  String get mimeType => throw _privateConstructorUsedError;
  String get fileExtension => throw _privateConstructorUsedError;
  int get fileSize => throw _privateConstructorUsedError;
  String? get hash => throw _privateConstructorUsedError;
  int? get width => throw _privateConstructorUsedError;
  int? get height => throw _privateConstructorUsedError;
  int? get duration => throw _privateConstructorUsedError;
  String get storageClass => throw _privateConstructorUsedError;
  String get filePurpose => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this StorageFile to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of StorageFile
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $StorageFileCopyWith<StorageFile> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StorageFileCopyWith<$Res> {
  factory $StorageFileCopyWith(
          StorageFile value, $Res Function(StorageFile) then) =
      _$StorageFileCopyWithImpl<$Res, StorageFile>;
  @useResult
  $Res call(
      {String id,
      String workspaceId,
      String provider,
      String bucket,
      String objectKey,
      String? cdnUrl,
      String mimeType,
      String fileExtension,
      int fileSize,
      String? hash,
      int? width,
      int? height,
      int? duration,
      String storageClass,
      String filePurpose,
      DateTime createdAt,
      DateTime updatedAt});
}

/// @nodoc
class _$StorageFileCopyWithImpl<$Res, $Val extends StorageFile>
    implements $StorageFileCopyWith<$Res> {
  _$StorageFileCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of StorageFile
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? workspaceId = null,
    Object? provider = null,
    Object? bucket = null,
    Object? objectKey = null,
    Object? cdnUrl = freezed,
    Object? mimeType = null,
    Object? fileExtension = null,
    Object? fileSize = null,
    Object? hash = freezed,
    Object? width = freezed,
    Object? height = freezed,
    Object? duration = freezed,
    Object? storageClass = null,
    Object? filePurpose = null,
    Object? createdAt = null,
    Object? updatedAt = null,
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
      provider: null == provider
          ? _value.provider
          : provider // ignore: cast_nullable_to_non_nullable
              as String,
      bucket: null == bucket
          ? _value.bucket
          : bucket // ignore: cast_nullable_to_non_nullable
              as String,
      objectKey: null == objectKey
          ? _value.objectKey
          : objectKey // ignore: cast_nullable_to_non_nullable
              as String,
      cdnUrl: freezed == cdnUrl
          ? _value.cdnUrl
          : cdnUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      mimeType: null == mimeType
          ? _value.mimeType
          : mimeType // ignore: cast_nullable_to_non_nullable
              as String,
      fileExtension: null == fileExtension
          ? _value.fileExtension
          : fileExtension // ignore: cast_nullable_to_non_nullable
              as String,
      fileSize: null == fileSize
          ? _value.fileSize
          : fileSize // ignore: cast_nullable_to_non_nullable
              as int,
      hash: freezed == hash
          ? _value.hash
          : hash // ignore: cast_nullable_to_non_nullable
              as String?,
      width: freezed == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as int?,
      height: freezed == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as int?,
      duration: freezed == duration
          ? _value.duration
          : duration // ignore: cast_nullable_to_non_nullable
              as int?,
      storageClass: null == storageClass
          ? _value.storageClass
          : storageClass // ignore: cast_nullable_to_non_nullable
              as String,
      filePurpose: null == filePurpose
          ? _value.filePurpose
          : filePurpose // ignore: cast_nullable_to_non_nullable
              as String,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: null == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StorageFileImplCopyWith<$Res>
    implements $StorageFileCopyWith<$Res> {
  factory _$$StorageFileImplCopyWith(
          _$StorageFileImpl value, $Res Function(_$StorageFileImpl) then) =
      __$$StorageFileImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String workspaceId,
      String provider,
      String bucket,
      String objectKey,
      String? cdnUrl,
      String mimeType,
      String fileExtension,
      int fileSize,
      String? hash,
      int? width,
      int? height,
      int? duration,
      String storageClass,
      String filePurpose,
      DateTime createdAt,
      DateTime updatedAt});
}

/// @nodoc
class __$$StorageFileImplCopyWithImpl<$Res>
    extends _$StorageFileCopyWithImpl<$Res, _$StorageFileImpl>
    implements _$$StorageFileImplCopyWith<$Res> {
  __$$StorageFileImplCopyWithImpl(
      _$StorageFileImpl _value, $Res Function(_$StorageFileImpl) _then)
      : super(_value, _then);

  /// Create a copy of StorageFile
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? workspaceId = null,
    Object? provider = null,
    Object? bucket = null,
    Object? objectKey = null,
    Object? cdnUrl = freezed,
    Object? mimeType = null,
    Object? fileExtension = null,
    Object? fileSize = null,
    Object? hash = freezed,
    Object? width = freezed,
    Object? height = freezed,
    Object? duration = freezed,
    Object? storageClass = null,
    Object? filePurpose = null,
    Object? createdAt = null,
    Object? updatedAt = null,
  }) {
    return _then(_$StorageFileImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      workspaceId: null == workspaceId
          ? _value.workspaceId
          : workspaceId // ignore: cast_nullable_to_non_nullable
              as String,
      provider: null == provider
          ? _value.provider
          : provider // ignore: cast_nullable_to_non_nullable
              as String,
      bucket: null == bucket
          ? _value.bucket
          : bucket // ignore: cast_nullable_to_non_nullable
              as String,
      objectKey: null == objectKey
          ? _value.objectKey
          : objectKey // ignore: cast_nullable_to_non_nullable
              as String,
      cdnUrl: freezed == cdnUrl
          ? _value.cdnUrl
          : cdnUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      mimeType: null == mimeType
          ? _value.mimeType
          : mimeType // ignore: cast_nullable_to_non_nullable
              as String,
      fileExtension: null == fileExtension
          ? _value.fileExtension
          : fileExtension // ignore: cast_nullable_to_non_nullable
              as String,
      fileSize: null == fileSize
          ? _value.fileSize
          : fileSize // ignore: cast_nullable_to_non_nullable
              as int,
      hash: freezed == hash
          ? _value.hash
          : hash // ignore: cast_nullable_to_non_nullable
              as String?,
      width: freezed == width
          ? _value.width
          : width // ignore: cast_nullable_to_non_nullable
              as int?,
      height: freezed == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as int?,
      duration: freezed == duration
          ? _value.duration
          : duration // ignore: cast_nullable_to_non_nullable
              as int?,
      storageClass: null == storageClass
          ? _value.storageClass
          : storageClass // ignore: cast_nullable_to_non_nullable
              as String,
      filePurpose: null == filePurpose
          ? _value.filePurpose
          : filePurpose // ignore: cast_nullable_to_non_nullable
              as String,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: null == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StorageFileImpl implements _StorageFile {
  const _$StorageFileImpl(
      {required this.id,
      required this.workspaceId,
      required this.provider,
      required this.bucket,
      required this.objectKey,
      this.cdnUrl,
      required this.mimeType,
      required this.fileExtension,
      required this.fileSize,
      this.hash,
      this.width,
      this.height,
      this.duration,
      this.storageClass = 'STANDARD',
      required this.filePurpose,
      required this.createdAt,
      required this.updatedAt});

  factory _$StorageFileImpl.fromJson(Map<String, dynamic> json) =>
      _$$StorageFileImplFromJson(json);

  @override
  final String id;
  @override
  final String workspaceId;
  @override
  final String provider;
  @override
  final String bucket;
  @override
  final String objectKey;
  @override
  final String? cdnUrl;
  @override
  final String mimeType;
  @override
  final String fileExtension;
  @override
  final int fileSize;
  @override
  final String? hash;
  @override
  final int? width;
  @override
  final int? height;
  @override
  final int? duration;
  @override
  @JsonKey()
  final String storageClass;
  @override
  final String filePurpose;
  @override
  final DateTime createdAt;
  @override
  final DateTime updatedAt;

  @override
  String toString() {
    return 'StorageFile(id: $id, workspaceId: $workspaceId, provider: $provider, bucket: $bucket, objectKey: $objectKey, cdnUrl: $cdnUrl, mimeType: $mimeType, fileExtension: $fileExtension, fileSize: $fileSize, hash: $hash, width: $width, height: $height, duration: $duration, storageClass: $storageClass, filePurpose: $filePurpose, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StorageFileImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.workspaceId, workspaceId) ||
                other.workspaceId == workspaceId) &&
            (identical(other.provider, provider) ||
                other.provider == provider) &&
            (identical(other.bucket, bucket) || other.bucket == bucket) &&
            (identical(other.objectKey, objectKey) ||
                other.objectKey == objectKey) &&
            (identical(other.cdnUrl, cdnUrl) || other.cdnUrl == cdnUrl) &&
            (identical(other.mimeType, mimeType) ||
                other.mimeType == mimeType) &&
            (identical(other.fileExtension, fileExtension) ||
                other.fileExtension == fileExtension) &&
            (identical(other.fileSize, fileSize) ||
                other.fileSize == fileSize) &&
            (identical(other.hash, hash) || other.hash == hash) &&
            (identical(other.width, width) || other.width == width) &&
            (identical(other.height, height) || other.height == height) &&
            (identical(other.duration, duration) ||
                other.duration == duration) &&
            (identical(other.storageClass, storageClass) ||
                other.storageClass == storageClass) &&
            (identical(other.filePurpose, filePurpose) ||
                other.filePurpose == filePurpose) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      workspaceId,
      provider,
      bucket,
      objectKey,
      cdnUrl,
      mimeType,
      fileExtension,
      fileSize,
      hash,
      width,
      height,
      duration,
      storageClass,
      filePurpose,
      createdAt,
      updatedAt);

  /// Create a copy of StorageFile
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$StorageFileImplCopyWith<_$StorageFileImpl> get copyWith =>
      __$$StorageFileImplCopyWithImpl<_$StorageFileImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StorageFileImplToJson(
      this,
    );
  }
}

abstract class _StorageFile implements StorageFile {
  const factory _StorageFile(
      {required final String id,
      required final String workspaceId,
      required final String provider,
      required final String bucket,
      required final String objectKey,
      final String? cdnUrl,
      required final String mimeType,
      required final String fileExtension,
      required final int fileSize,
      final String? hash,
      final int? width,
      final int? height,
      final int? duration,
      final String storageClass,
      required final String filePurpose,
      required final DateTime createdAt,
      required final DateTime updatedAt}) = _$StorageFileImpl;

  factory _StorageFile.fromJson(Map<String, dynamic> json) =
      _$StorageFileImpl.fromJson;

  @override
  String get id;
  @override
  String get workspaceId;
  @override
  String get provider;
  @override
  String get bucket;
  @override
  String get objectKey;
  @override
  String? get cdnUrl;
  @override
  String get mimeType;
  @override
  String get fileExtension;
  @override
  int get fileSize;
  @override
  String? get hash;
  @override
  int? get width;
  @override
  int? get height;
  @override
  int? get duration;
  @override
  String get storageClass;
  @override
  String get filePurpose;
  @override
  DateTime get createdAt;
  @override
  DateTime get updatedAt;

  /// Create a copy of StorageFile
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$StorageFileImplCopyWith<_$StorageFileImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Asset _$AssetFromJson(Map<String, dynamic> json) {
  return _Asset.fromJson(json);
}

/// @nodoc
mixin _$Asset {
  String get id => throw _privateConstructorUsedError;
  String get workspaceId => throw _privateConstructorUsedError;
  String get brandId => throw _privateConstructorUsedError;
  String get productServiceId => throw _privateConstructorUsedError;
  String get projectCampaignId => throw _privateConstructorUsedError;
  String get storageFileId => throw _privateConstructorUsedError;
  String get uploadedBy => throw _privateConstructorUsedError;
  AssetType get assetType => throw _privateConstructorUsedError;
  AssetCategory get assetCategory => throw _privateConstructorUsedError;
  String get originalFileName => throw _privateConstructorUsedError;
  String get displayName => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  List<String> get tags => throw _privateConstructorUsedError;
  AssetStatus get status => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime get updatedAt => throw _privateConstructorUsedError;
  StorageFile? get file => throw _privateConstructorUsedError;

  /// Serializes this Asset to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Asset
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $AssetCopyWith<Asset> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $AssetCopyWith<$Res> {
  factory $AssetCopyWith(Asset value, $Res Function(Asset) then) =
      _$AssetCopyWithImpl<$Res, Asset>;
  @useResult
  $Res call(
      {String id,
      String workspaceId,
      String brandId,
      String productServiceId,
      String projectCampaignId,
      String storageFileId,
      String uploadedBy,
      AssetType assetType,
      AssetCategory assetCategory,
      String originalFileName,
      String displayName,
      String? description,
      List<String> tags,
      AssetStatus status,
      DateTime createdAt,
      DateTime updatedAt,
      StorageFile? file});

  $StorageFileCopyWith<$Res>? get file;
}

/// @nodoc
class _$AssetCopyWithImpl<$Res, $Val extends Asset>
    implements $AssetCopyWith<$Res> {
  _$AssetCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Asset
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? workspaceId = null,
    Object? brandId = null,
    Object? productServiceId = null,
    Object? projectCampaignId = null,
    Object? storageFileId = null,
    Object? uploadedBy = null,
    Object? assetType = null,
    Object? assetCategory = null,
    Object? originalFileName = null,
    Object? displayName = null,
    Object? description = freezed,
    Object? tags = null,
    Object? status = null,
    Object? createdAt = null,
    Object? updatedAt = null,
    Object? file = freezed,
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
      projectCampaignId: null == projectCampaignId
          ? _value.projectCampaignId
          : projectCampaignId // ignore: cast_nullable_to_non_nullable
              as String,
      storageFileId: null == storageFileId
          ? _value.storageFileId
          : storageFileId // ignore: cast_nullable_to_non_nullable
              as String,
      uploadedBy: null == uploadedBy
          ? _value.uploadedBy
          : uploadedBy // ignore: cast_nullable_to_non_nullable
              as String,
      assetType: null == assetType
          ? _value.assetType
          : assetType // ignore: cast_nullable_to_non_nullable
              as AssetType,
      assetCategory: null == assetCategory
          ? _value.assetCategory
          : assetCategory // ignore: cast_nullable_to_non_nullable
              as AssetCategory,
      originalFileName: null == originalFileName
          ? _value.originalFileName
          : originalFileName // ignore: cast_nullable_to_non_nullable
              as String,
      displayName: null == displayName
          ? _value.displayName
          : displayName // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      tags: null == tags
          ? _value.tags
          : tags // ignore: cast_nullable_to_non_nullable
              as List<String>,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as AssetStatus,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: null == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      file: freezed == file
          ? _value.file
          : file // ignore: cast_nullable_to_non_nullable
              as StorageFile?,
    ) as $Val);
  }

  /// Create a copy of Asset
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $StorageFileCopyWith<$Res>? get file {
    if (_value.file == null) {
      return null;
    }

    return $StorageFileCopyWith<$Res>(_value.file!, (value) {
      return _then(_value.copyWith(file: value) as $Val);
    });
  }
}

/// @nodoc
abstract class _$$AssetImplCopyWith<$Res> implements $AssetCopyWith<$Res> {
  factory _$$AssetImplCopyWith(
          _$AssetImpl value, $Res Function(_$AssetImpl) then) =
      __$$AssetImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String workspaceId,
      String brandId,
      String productServiceId,
      String projectCampaignId,
      String storageFileId,
      String uploadedBy,
      AssetType assetType,
      AssetCategory assetCategory,
      String originalFileName,
      String displayName,
      String? description,
      List<String> tags,
      AssetStatus status,
      DateTime createdAt,
      DateTime updatedAt,
      StorageFile? file});

  @override
  $StorageFileCopyWith<$Res>? get file;
}

/// @nodoc
class __$$AssetImplCopyWithImpl<$Res>
    extends _$AssetCopyWithImpl<$Res, _$AssetImpl>
    implements _$$AssetImplCopyWith<$Res> {
  __$$AssetImplCopyWithImpl(
      _$AssetImpl _value, $Res Function(_$AssetImpl) _then)
      : super(_value, _then);

  /// Create a copy of Asset
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? workspaceId = null,
    Object? brandId = null,
    Object? productServiceId = null,
    Object? projectCampaignId = null,
    Object? storageFileId = null,
    Object? uploadedBy = null,
    Object? assetType = null,
    Object? assetCategory = null,
    Object? originalFileName = null,
    Object? displayName = null,
    Object? description = freezed,
    Object? tags = null,
    Object? status = null,
    Object? createdAt = null,
    Object? updatedAt = null,
    Object? file = freezed,
  }) {
    return _then(_$AssetImpl(
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
      projectCampaignId: null == projectCampaignId
          ? _value.projectCampaignId
          : projectCampaignId // ignore: cast_nullable_to_non_nullable
              as String,
      storageFileId: null == storageFileId
          ? _value.storageFileId
          : storageFileId // ignore: cast_nullable_to_non_nullable
              as String,
      uploadedBy: null == uploadedBy
          ? _value.uploadedBy
          : uploadedBy // ignore: cast_nullable_to_non_nullable
              as String,
      assetType: null == assetType
          ? _value.assetType
          : assetType // ignore: cast_nullable_to_non_nullable
              as AssetType,
      assetCategory: null == assetCategory
          ? _value.assetCategory
          : assetCategory // ignore: cast_nullable_to_non_nullable
              as AssetCategory,
      originalFileName: null == originalFileName
          ? _value.originalFileName
          : originalFileName // ignore: cast_nullable_to_non_nullable
              as String,
      displayName: null == displayName
          ? _value.displayName
          : displayName // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      tags: null == tags
          ? _value._tags
          : tags // ignore: cast_nullable_to_non_nullable
              as List<String>,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as AssetStatus,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: null == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      file: freezed == file
          ? _value.file
          : file // ignore: cast_nullable_to_non_nullable
              as StorageFile?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$AssetImpl implements _Asset {
  const _$AssetImpl(
      {required this.id,
      required this.workspaceId,
      required this.brandId,
      required this.productServiceId,
      required this.projectCampaignId,
      required this.storageFileId,
      required this.uploadedBy,
      required this.assetType,
      required this.assetCategory,
      required this.originalFileName,
      required this.displayName,
      this.description,
      final List<String> tags = const [],
      this.status = AssetStatus.active,
      required this.createdAt,
      required this.updatedAt,
      this.file})
      : _tags = tags;

  factory _$AssetImpl.fromJson(Map<String, dynamic> json) =>
      _$$AssetImplFromJson(json);

  @override
  final String id;
  @override
  final String workspaceId;
  @override
  final String brandId;
  @override
  final String productServiceId;
  @override
  final String projectCampaignId;
  @override
  final String storageFileId;
  @override
  final String uploadedBy;
  @override
  final AssetType assetType;
  @override
  final AssetCategory assetCategory;
  @override
  final String originalFileName;
  @override
  final String displayName;
  @override
  final String? description;
  final List<String> _tags;
  @override
  @JsonKey()
  List<String> get tags {
    if (_tags is EqualUnmodifiableListView) return _tags;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_tags);
  }

  @override
  @JsonKey()
  final AssetStatus status;
  @override
  final DateTime createdAt;
  @override
  final DateTime updatedAt;
  @override
  final StorageFile? file;

  @override
  String toString() {
    return 'Asset(id: $id, workspaceId: $workspaceId, brandId: $brandId, productServiceId: $productServiceId, projectCampaignId: $projectCampaignId, storageFileId: $storageFileId, uploadedBy: $uploadedBy, assetType: $assetType, assetCategory: $assetCategory, originalFileName: $originalFileName, displayName: $displayName, description: $description, tags: $tags, status: $status, createdAt: $createdAt, updatedAt: $updatedAt, file: $file)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$AssetImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.workspaceId, workspaceId) ||
                other.workspaceId == workspaceId) &&
            (identical(other.brandId, brandId) || other.brandId == brandId) &&
            (identical(other.productServiceId, productServiceId) ||
                other.productServiceId == productServiceId) &&
            (identical(other.projectCampaignId, projectCampaignId) ||
                other.projectCampaignId == projectCampaignId) &&
            (identical(other.storageFileId, storageFileId) ||
                other.storageFileId == storageFileId) &&
            (identical(other.uploadedBy, uploadedBy) ||
                other.uploadedBy == uploadedBy) &&
            (identical(other.assetType, assetType) ||
                other.assetType == assetType) &&
            (identical(other.assetCategory, assetCategory) ||
                other.assetCategory == assetCategory) &&
            (identical(other.originalFileName, originalFileName) ||
                other.originalFileName == originalFileName) &&
            (identical(other.displayName, displayName) ||
                other.displayName == displayName) &&
            (identical(other.description, description) ||
                other.description == description) &&
            const DeepCollectionEquality().equals(other._tags, _tags) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt) &&
            (identical(other.file, file) || other.file == file));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      workspaceId,
      brandId,
      productServiceId,
      projectCampaignId,
      storageFileId,
      uploadedBy,
      assetType,
      assetCategory,
      originalFileName,
      displayName,
      description,
      const DeepCollectionEquality().hash(_tags),
      status,
      createdAt,
      updatedAt,
      file);

  /// Create a copy of Asset
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$AssetImplCopyWith<_$AssetImpl> get copyWith =>
      __$$AssetImplCopyWithImpl<_$AssetImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$AssetImplToJson(
      this,
    );
  }
}

abstract class _Asset implements Asset {
  const factory _Asset(
      {required final String id,
      required final String workspaceId,
      required final String brandId,
      required final String productServiceId,
      required final String projectCampaignId,
      required final String storageFileId,
      required final String uploadedBy,
      required final AssetType assetType,
      required final AssetCategory assetCategory,
      required final String originalFileName,
      required final String displayName,
      final String? description,
      final List<String> tags,
      final AssetStatus status,
      required final DateTime createdAt,
      required final DateTime updatedAt,
      final StorageFile? file}) = _$AssetImpl;

  factory _Asset.fromJson(Map<String, dynamic> json) = _$AssetImpl.fromJson;

  @override
  String get id;
  @override
  String get workspaceId;
  @override
  String get brandId;
  @override
  String get productServiceId;
  @override
  String get projectCampaignId;
  @override
  String get storageFileId;
  @override
  String get uploadedBy;
  @override
  AssetType get assetType;
  @override
  AssetCategory get assetCategory;
  @override
  String get originalFileName;
  @override
  String get displayName;
  @override
  String? get description;
  @override
  List<String> get tags;
  @override
  AssetStatus get status;
  @override
  DateTime get createdAt;
  @override
  DateTime get updatedAt;
  @override
  StorageFile? get file;

  /// Create a copy of Asset
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$AssetImplCopyWith<_$AssetImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

AssetUploadRequest _$AssetUploadRequestFromJson(Map<String, dynamic> json) {
  return _AssetUploadRequest.fromJson(json);
}

/// @nodoc
mixin _$AssetUploadRequest {
  AssetCategory get assetCategory => throw _privateConstructorUsedError;
  String get displayName => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  List<String> get tags => throw _privateConstructorUsedError;

  /// Serializes this AssetUploadRequest to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of AssetUploadRequest
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $AssetUploadRequestCopyWith<AssetUploadRequest> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $AssetUploadRequestCopyWith<$Res> {
  factory $AssetUploadRequestCopyWith(
          AssetUploadRequest value, $Res Function(AssetUploadRequest) then) =
      _$AssetUploadRequestCopyWithImpl<$Res, AssetUploadRequest>;
  @useResult
  $Res call(
      {AssetCategory assetCategory,
      String displayName,
      String? description,
      List<String> tags});
}

/// @nodoc
class _$AssetUploadRequestCopyWithImpl<$Res, $Val extends AssetUploadRequest>
    implements $AssetUploadRequestCopyWith<$Res> {
  _$AssetUploadRequestCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of AssetUploadRequest
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? assetCategory = null,
    Object? displayName = null,
    Object? description = freezed,
    Object? tags = null,
  }) {
    return _then(_value.copyWith(
      assetCategory: null == assetCategory
          ? _value.assetCategory
          : assetCategory // ignore: cast_nullable_to_non_nullable
              as AssetCategory,
      displayName: null == displayName
          ? _value.displayName
          : displayName // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      tags: null == tags
          ? _value.tags
          : tags // ignore: cast_nullable_to_non_nullable
              as List<String>,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$AssetUploadRequestImplCopyWith<$Res>
    implements $AssetUploadRequestCopyWith<$Res> {
  factory _$$AssetUploadRequestImplCopyWith(_$AssetUploadRequestImpl value,
          $Res Function(_$AssetUploadRequestImpl) then) =
      __$$AssetUploadRequestImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {AssetCategory assetCategory,
      String displayName,
      String? description,
      List<String> tags});
}

/// @nodoc
class __$$AssetUploadRequestImplCopyWithImpl<$Res>
    extends _$AssetUploadRequestCopyWithImpl<$Res, _$AssetUploadRequestImpl>
    implements _$$AssetUploadRequestImplCopyWith<$Res> {
  __$$AssetUploadRequestImplCopyWithImpl(_$AssetUploadRequestImpl _value,
      $Res Function(_$AssetUploadRequestImpl) _then)
      : super(_value, _then);

  /// Create a copy of AssetUploadRequest
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? assetCategory = null,
    Object? displayName = null,
    Object? description = freezed,
    Object? tags = null,
  }) {
    return _then(_$AssetUploadRequestImpl(
      assetCategory: null == assetCategory
          ? _value.assetCategory
          : assetCategory // ignore: cast_nullable_to_non_nullable
              as AssetCategory,
      displayName: null == displayName
          ? _value.displayName
          : displayName // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      tags: null == tags
          ? _value._tags
          : tags // ignore: cast_nullable_to_non_nullable
              as List<String>,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$AssetUploadRequestImpl implements _AssetUploadRequest {
  const _$AssetUploadRequestImpl(
      {required this.assetCategory,
      required this.displayName,
      this.description,
      final List<String> tags = const []})
      : _tags = tags;

  factory _$AssetUploadRequestImpl.fromJson(Map<String, dynamic> json) =>
      _$$AssetUploadRequestImplFromJson(json);

  @override
  final AssetCategory assetCategory;
  @override
  final String displayName;
  @override
  final String? description;
  final List<String> _tags;
  @override
  @JsonKey()
  List<String> get tags {
    if (_tags is EqualUnmodifiableListView) return _tags;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_tags);
  }

  @override
  String toString() {
    return 'AssetUploadRequest(assetCategory: $assetCategory, displayName: $displayName, description: $description, tags: $tags)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$AssetUploadRequestImpl &&
            (identical(other.assetCategory, assetCategory) ||
                other.assetCategory == assetCategory) &&
            (identical(other.displayName, displayName) ||
                other.displayName == displayName) &&
            (identical(other.description, description) ||
                other.description == description) &&
            const DeepCollectionEquality().equals(other._tags, _tags));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, assetCategory, displayName,
      description, const DeepCollectionEquality().hash(_tags));

  /// Create a copy of AssetUploadRequest
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$AssetUploadRequestImplCopyWith<_$AssetUploadRequestImpl> get copyWith =>
      __$$AssetUploadRequestImplCopyWithImpl<_$AssetUploadRequestImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$AssetUploadRequestImplToJson(
      this,
    );
  }
}

abstract class _AssetUploadRequest implements AssetUploadRequest {
  const factory _AssetUploadRequest(
      {required final AssetCategory assetCategory,
      required final String displayName,
      final String? description,
      final List<String> tags}) = _$AssetUploadRequestImpl;

  factory _AssetUploadRequest.fromJson(Map<String, dynamic> json) =
      _$AssetUploadRequestImpl.fromJson;

  @override
  AssetCategory get assetCategory;
  @override
  String get displayName;
  @override
  String? get description;
  @override
  List<String> get tags;

  /// Create a copy of AssetUploadRequest
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$AssetUploadRequestImplCopyWith<_$AssetUploadRequestImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
