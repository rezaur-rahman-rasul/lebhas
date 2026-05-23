import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/api/api_client.dart';
import '../domain/auth_models.dart';

final authApiProvider = Provider<AuthApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return AuthApi(apiClient);
});

class AuthApi {
  final ApiClient _apiClient;

  AuthApi(this._apiClient);

  Future<Map<String, dynamic>> login(LoginRequest request) async {
    final response = await _apiClient.post('/auth/login', data: request.toJson());
    return response.data;
  }

  Future<Map<String, dynamic>> register(RegisterRequest request) async {
    final response = await _apiClient.post('/auth/register', data: request.toJson());
    return response.data;
  }

  Future<Map<String, dynamic>> refreshToken(String refreshToken) async {
    final response = await _apiClient.post('/auth/refresh', data: {'refreshToken': refreshToken});
    return response.data;
  }

  Future<void> logout() async {
    await _apiClient.post('/auth/logout');
  }

  Future<Map<String, dynamic>> getCurrentUser() async {
    final response = await _apiClient.get('/auth/me');
    return response.data;
  }
}
