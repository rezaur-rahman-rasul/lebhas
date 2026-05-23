// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'permission_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

PermissionSnapshot _$PermissionSnapshotFromJson(Map<String, dynamic> json) {
  return _PermissionSnapshot.fromJson(json);
}

/// @nodoc
mixin _$PermissionSnapshot {
  List<String> get globalPermissions => throw _privateConstructorUsedError;
  Map<String, List<String>> get workspacePermissions =>
      throw _privateConstructorUsedError;

  /// Serializes this PermissionSnapshot to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of PermissionSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $PermissionSnapshotCopyWith<PermissionSnapshot> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $PermissionSnapshotCopyWith<$Res> {
  factory $PermissionSnapshotCopyWith(
          PermissionSnapshot value, $Res Function(PermissionSnapshot) then) =
      _$PermissionSnapshotCopyWithImpl<$Res, PermissionSnapshot>;
  @useResult
  $Res call(
      {List<String> globalPermissions,
      Map<String, List<String>> workspacePermissions});
}

/// @nodoc
class _$PermissionSnapshotCopyWithImpl<$Res, $Val extends PermissionSnapshot>
    implements $PermissionSnapshotCopyWith<$Res> {
  _$PermissionSnapshotCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of PermissionSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? globalPermissions = null,
    Object? workspacePermissions = null,
  }) {
    return _then(_value.copyWith(
      globalPermissions: null == globalPermissions
          ? _value.globalPermissions
          : globalPermissions // ignore: cast_nullable_to_non_nullable
              as List<String>,
      workspacePermissions: null == workspacePermissions
          ? _value.workspacePermissions
          : workspacePermissions // ignore: cast_nullable_to_non_nullable
              as Map<String, List<String>>,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$PermissionSnapshotImplCopyWith<$Res>
    implements $PermissionSnapshotCopyWith<$Res> {
  factory _$$PermissionSnapshotImplCopyWith(_$PermissionSnapshotImpl value,
          $Res Function(_$PermissionSnapshotImpl) then) =
      __$$PermissionSnapshotImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {List<String> globalPermissions,
      Map<String, List<String>> workspacePermissions});
}

/// @nodoc
class __$$PermissionSnapshotImplCopyWithImpl<$Res>
    extends _$PermissionSnapshotCopyWithImpl<$Res, _$PermissionSnapshotImpl>
    implements _$$PermissionSnapshotImplCopyWith<$Res> {
  __$$PermissionSnapshotImplCopyWithImpl(_$PermissionSnapshotImpl _value,
      $Res Function(_$PermissionSnapshotImpl) _then)
      : super(_value, _then);

  /// Create a copy of PermissionSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? globalPermissions = null,
    Object? workspacePermissions = null,
  }) {
    return _then(_$PermissionSnapshotImpl(
      globalPermissions: null == globalPermissions
          ? _value._globalPermissions
          : globalPermissions // ignore: cast_nullable_to_non_nullable
              as List<String>,
      workspacePermissions: null == workspacePermissions
          ? _value._workspacePermissions
          : workspacePermissions // ignore: cast_nullable_to_non_nullable
              as Map<String, List<String>>,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$PermissionSnapshotImpl implements _PermissionSnapshot {
  const _$PermissionSnapshotImpl(
      {required final List<String> globalPermissions,
      required final Map<String, List<String>> workspacePermissions})
      : _globalPermissions = globalPermissions,
        _workspacePermissions = workspacePermissions;

  factory _$PermissionSnapshotImpl.fromJson(Map<String, dynamic> json) =>
      _$$PermissionSnapshotImplFromJson(json);

  final List<String> _globalPermissions;
  @override
  List<String> get globalPermissions {
    if (_globalPermissions is EqualUnmodifiableListView)
      return _globalPermissions;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_globalPermissions);
  }

  final Map<String, List<String>> _workspacePermissions;
  @override
  Map<String, List<String>> get workspacePermissions {
    if (_workspacePermissions is EqualUnmodifiableMapView)
      return _workspacePermissions;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableMapView(_workspacePermissions);
  }

  @override
  String toString() {
    return 'PermissionSnapshot(globalPermissions: $globalPermissions, workspacePermissions: $workspacePermissions)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$PermissionSnapshotImpl &&
            const DeepCollectionEquality()
                .equals(other._globalPermissions, _globalPermissions) &&
            const DeepCollectionEquality()
                .equals(other._workspacePermissions, _workspacePermissions));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      const DeepCollectionEquality().hash(_globalPermissions),
      const DeepCollectionEquality().hash(_workspacePermissions));

  /// Create a copy of PermissionSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$PermissionSnapshotImplCopyWith<_$PermissionSnapshotImpl> get copyWith =>
      __$$PermissionSnapshotImplCopyWithImpl<_$PermissionSnapshotImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$PermissionSnapshotImplToJson(
      this,
    );
  }
}

abstract class _PermissionSnapshot implements PermissionSnapshot {
  const factory _PermissionSnapshot(
          {required final List<String> globalPermissions,
          required final Map<String, List<String>> workspacePermissions}) =
      _$PermissionSnapshotImpl;

  factory _PermissionSnapshot.fromJson(Map<String, dynamic> json) =
      _$PermissionSnapshotImpl.fromJson;

  @override
  List<String> get globalPermissions;
  @override
  Map<String, List<String>> get workspacePermissions;

  /// Create a copy of PermissionSnapshot
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$PermissionSnapshotImplCopyWith<_$PermissionSnapshotImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
