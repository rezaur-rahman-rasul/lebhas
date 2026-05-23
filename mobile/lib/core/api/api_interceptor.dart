import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/auth/state/auth_controller.dart';
import '../storage/secure_storage_service.dart';
import '../constants/storage_keys.dart';

class AuthInterceptor extends Interceptor {
  final SecureStorageService _storage;
  final Ref _ref;

  AuthInterceptor(this._storage, this._ref);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    final token = await _storage.read(StorageKeys.authToken);
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    options.headers['X-Correlation-ID'] = DateTime.now().millisecondsSinceEpoch.toString();
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode == 401) {
      // Logic for token refresh would go here
      // For now, if we get 401, we logout the user to be safe
      _ref.read(authProvider.notifier).logout();
    }
    handler.next(err);
  }
}
