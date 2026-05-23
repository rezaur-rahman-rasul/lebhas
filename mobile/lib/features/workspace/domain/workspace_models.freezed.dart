// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'workspace_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

WorkspaceContext _$WorkspaceContextFromJson(Map<String, dynamic> json) {
  return _WorkspaceContext.fromJson(json);
}

/// @nodoc
mixin _$WorkspaceContext {
  String get workspaceId => throw _privateConstructorUsedError;
  String get workspaceName => throw _privateConstructorUsedError;
  String get role => throw _privateConstructorUsedError;
  List<String> get permissions => throw _privateConstructorUsedError;
  bool get supportMode => throw _privateConstructorUsedError;

  /// Serializes this WorkspaceContext to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of WorkspaceContext
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $WorkspaceContextCopyWith<WorkspaceContext> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $WorkspaceContextCopyWith<$Res> {
  factory $WorkspaceContextCopyWith(
          WorkspaceContext value, $Res Function(WorkspaceContext) then) =
      _$WorkspaceContextCopyWithImpl<$Res, WorkspaceContext>;
  @useResult
  $Res call(
      {String workspaceId,
      String workspaceName,
      String role,
      List<String> permissions,
      bool supportMode});
}

/// @nodoc
class _$WorkspaceContextCopyWithImpl<$Res, $Val extends WorkspaceContext>
    implements $WorkspaceContextCopyWith<$Res> {
  _$WorkspaceContextCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of WorkspaceContext
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? workspaceId = null,
    Object? workspaceName = null,
    Object? role = null,
    Object? permissions = null,
    Object? supportMode = null,
  }) {
    return _then(_value.copyWith(
      workspaceId: null == workspaceId
          ? _value.workspaceId
          : workspaceId // ignore: cast_nullable_to_non_nullable
              as String,
      workspaceName: null == workspaceName
          ? _value.workspaceName
          : workspaceName // ignore: cast_nullable_to_non_nullable
              as String,
      role: null == role
          ? _value.role
          : role // ignore: cast_nullable_to_non_nullable
              as String,
      permissions: null == permissions
          ? _value.permissions
          : permissions // ignore: cast_nullable_to_non_nullable
              as List<String>,
      supportMode: null == supportMode
          ? _value.supportMode
          : supportMode // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$WorkspaceContextImplCopyWith<$Res>
    implements $WorkspaceContextCopyWith<$Res> {
  factory _$$WorkspaceContextImplCopyWith(_$WorkspaceContextImpl value,
          $Res Function(_$WorkspaceContextImpl) then) =
      __$$WorkspaceContextImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String workspaceId,
      String workspaceName,
      String role,
      List<String> permissions,
      bool supportMode});
}

/// @nodoc
class __$$WorkspaceContextImplCopyWithImpl<$Res>
    extends _$WorkspaceContextCopyWithImpl<$Res, _$WorkspaceContextImpl>
    implements _$$WorkspaceContextImplCopyWith<$Res> {
  __$$WorkspaceContextImplCopyWithImpl(_$WorkspaceContextImpl _value,
      $Res Function(_$WorkspaceContextImpl) _then)
      : super(_value, _then);

  /// Create a copy of WorkspaceContext
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? workspaceId = null,
    Object? workspaceName = null,
    Object? role = null,
    Object? permissions = null,
    Object? supportMode = null,
  }) {
    return _then(_$WorkspaceContextImpl(
      workspaceId: null == workspaceId
          ? _value.workspaceId
          : workspaceId // ignore: cast_nullable_to_non_nullable
              as String,
      workspaceName: null == workspaceName
          ? _value.workspaceName
          : workspaceName // ignore: cast_nullable_to_non_nullable
              as String,
      role: null == role
          ? _value.role
          : role // ignore: cast_nullable_to_non_nullable
              as String,
      permissions: null == permissions
          ? _value._permissions
          : permissions // ignore: cast_nullable_to_non_nullable
              as List<String>,
      supportMode: null == supportMode
          ? _value.supportMode
          : supportMode // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$WorkspaceContextImpl implements _WorkspaceContext {
  const _$WorkspaceContextImpl(
      {required this.workspaceId,
      required this.workspaceName,
      required this.role,
      final List<String> permissions = const [],
      this.supportMode = false})
      : _permissions = permissions;

  factory _$WorkspaceContextImpl.fromJson(Map<String, dynamic> json) =>
      _$$WorkspaceContextImplFromJson(json);

  @override
  final String workspaceId;
  @override
  final String workspaceName;
  @override
  final String role;
  final List<String> _permissions;
  @override
  @JsonKey()
  List<String> get permissions {
    if (_permissions is EqualUnmodifiableListView) return _permissions;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_permissions);
  }

  @override
  @JsonKey()
  final bool supportMode;

  @override
  String toString() {
    return 'WorkspaceContext(workspaceId: $workspaceId, workspaceName: $workspaceName, role: $role, permissions: $permissions, supportMode: $supportMode)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$WorkspaceContextImpl &&
            (identical(other.workspaceId, workspaceId) ||
                other.workspaceId == workspaceId) &&
            (identical(other.workspaceName, workspaceName) ||
                other.workspaceName == workspaceName) &&
            (identical(other.role, role) || other.role == role) &&
            const DeepCollectionEquality()
                .equals(other._permissions, _permissions) &&
            (identical(other.supportMode, supportMode) ||
                other.supportMode == supportMode));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, workspaceId, workspaceName, role,
      const DeepCollectionEquality().hash(_permissions), supportMode);

  /// Create a copy of WorkspaceContext
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$WorkspaceContextImplCopyWith<_$WorkspaceContextImpl> get copyWith =>
      __$$WorkspaceContextImplCopyWithImpl<_$WorkspaceContextImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$WorkspaceContextImplToJson(
      this,
    );
  }
}

abstract class _WorkspaceContext implements WorkspaceContext {
  const factory _WorkspaceContext(
      {required final String workspaceId,
      required final String workspaceName,
      required final String role,
      final List<String> permissions,
      final bool supportMode}) = _$WorkspaceContextImpl;

  factory _WorkspaceContext.fromJson(Map<String, dynamic> json) =
      _$WorkspaceContextImpl.fromJson;

  @override
  String get workspaceId;
  @override
  String get workspaceName;
  @override
  String get role;
  @override
  List<String> get permissions;
  @override
  bool get supportMode;

  /// Create a copy of WorkspaceContext
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$WorkspaceContextImplCopyWith<_$WorkspaceContextImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Workspace _$WorkspaceFromJson(Map<String, dynamic> json) {
  return _Workspace.fromJson(json);
}

/// @nodoc
mixin _$Workspace {
  String get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  bool get isDefault => throw _privateConstructorUsedError;

  /// Serializes this Workspace to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Workspace
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $WorkspaceCopyWith<Workspace> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $WorkspaceCopyWith<$Res> {
  factory $WorkspaceCopyWith(Workspace value, $Res Function(Workspace) then) =
      _$WorkspaceCopyWithImpl<$Res, Workspace>;
  @useResult
  $Res call({String id, String name, String? description, bool isDefault});
}

/// @nodoc
class _$WorkspaceCopyWithImpl<$Res, $Val extends Workspace>
    implements $WorkspaceCopyWith<$Res> {
  _$WorkspaceCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Workspace
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? isDefault = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      isDefault: null == isDefault
          ? _value.isDefault
          : isDefault // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$WorkspaceImplCopyWith<$Res>
    implements $WorkspaceCopyWith<$Res> {
  factory _$$WorkspaceImplCopyWith(
          _$WorkspaceImpl value, $Res Function(_$WorkspaceImpl) then) =
      __$$WorkspaceImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String id, String name, String? description, bool isDefault});
}

/// @nodoc
class __$$WorkspaceImplCopyWithImpl<$Res>
    extends _$WorkspaceCopyWithImpl<$Res, _$WorkspaceImpl>
    implements _$$WorkspaceImplCopyWith<$Res> {
  __$$WorkspaceImplCopyWithImpl(
      _$WorkspaceImpl _value, $Res Function(_$WorkspaceImpl) _then)
      : super(_value, _then);

  /// Create a copy of Workspace
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? isDefault = null,
  }) {
    return _then(_$WorkspaceImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      isDefault: null == isDefault
          ? _value.isDefault
          : isDefault // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$WorkspaceImpl implements _Workspace {
  const _$WorkspaceImpl(
      {required this.id,
      required this.name,
      this.description,
      this.isDefault = false});

  factory _$WorkspaceImpl.fromJson(Map<String, dynamic> json) =>
      _$$WorkspaceImplFromJson(json);

  @override
  final String id;
  @override
  final String name;
  @override
  final String? description;
  @override
  @JsonKey()
  final bool isDefault;

  @override
  String toString() {
    return 'Workspace(id: $id, name: $name, description: $description, isDefault: $isDefault)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$WorkspaceImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.isDefault, isDefault) ||
                other.isDefault == isDefault));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, name, description, isDefault);

  /// Create a copy of Workspace
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$WorkspaceImplCopyWith<_$WorkspaceImpl> get copyWith =>
      __$$WorkspaceImplCopyWithImpl<_$WorkspaceImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$WorkspaceImplToJson(
      this,
    );
  }
}

abstract class _Workspace implements Workspace {
  const factory _Workspace(
      {required final String id,
      required final String name,
      final String? description,
      final bool isDefault}) = _$WorkspaceImpl;

  factory _Workspace.fromJson(Map<String, dynamic> json) =
      _$WorkspaceImpl.fromJson;

  @override
  String get id;
  @override
  String get name;
  @override
  String? get description;
  @override
  bool get isDefault;

  /// Create a copy of Workspace
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$WorkspaceImplCopyWith<_$WorkspaceImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
