import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_client.dart';
import '../../../core/api/api_response.dart';
import '../../../core/constants/storage_keys.dart';
import '../../../core/storage/secure_storage_service.dart';
import '../domain/auth_models.dart';
import '../domain/user_models.dart';
import 'auth_api.dart';

final authApiProvider = Provider<AuthApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return AuthApi(apiClient);
});

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  final authApi = ref.watch(authApiProvider);
  final storage = ref.watch(secureStorageServiceProvider);
  return AuthRepository(authApi, storage);
});

class AuthRepository {
  final AuthApi _authApi;
  final SecureStorageService _storage;

  AuthRepository(this._authApi, this._storage);

  Future<AuthResponse> login(LoginRequest request) async {
    final rawResponse = await _authApi.login(request);
    final response = ApiResponse<AuthResponse>.fromJson(
      rawResponse,
      (json) => AuthResponse.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      final authData = response.data!;
      await _saveAuthData(authData);
      return authData;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Login failed');
    }
  }

  Future<AuthResponse> register(RegisterRequest request) async {
    final rawResponse = await _authApi.register(request);
    final response = ApiResponse<AuthResponse>.fromJson(
      rawResponse,
      (json) => AuthResponse.fromJson(json as Map<String, dynamic>),
    );

    if (response.success && response.data != null) {
      final authData = response.data!;
      await _saveAuthData(authData);
      return authData;
    } else {
      throw Exception(response.errors.isNotEmpty ? response.errors.first : 'Registration failed');
    }
  }

  Future<void> _saveAuthData(AuthResponse authData) async {
    await _storage.write(StorageKeys.authToken, authData.accessToken);
    await _storage.write(StorageKeys.refreshToken, authData.refreshToken);
    await _storage.write(StorageKeys.user, jsonEncode(authData.user.toJson()));
  }

  Future<void> logout() async {
    try {
      await _authApi.logout();
    } finally {
      await _storage.deleteAll();
    }
  }

  Future<User?> getCurrentUser() async {
    final userJson = await _storage.read(StorageKeys.user);
    if (userJson != null) {
      return User.fromJson(jsonDecode(userJson));
    }
    return null;
  }
  
  Future<String?> getAuthToken() async {
    return await _storage.read(StorageKeys.authToken);
  }
}
