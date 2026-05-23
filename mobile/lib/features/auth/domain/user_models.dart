import 'package:freezed_annotation/freezed_annotation.dart';

part 'user_models.freezed.dart';
part 'user_models.g.dart';

@freezed
class User with _$User {
  const User._();

  const factory User({
    required String id,
    required String email,
    required String firstName,
    required String lastName,
    String? phone,
    String? avatarUrl,
    required String role,
    @Default([]) List<String> permissions,
  }) = _User;

  String get fullName => '$firstName $lastName';

  factory User.fromJson(Map<String, dynamic> json) => _$UserFromJson(json);
}
