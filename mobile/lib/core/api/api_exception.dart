import 'package:dio/dio.dart';

class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final dynamic data;

  ApiException({required this.message, this.statusCode, this.data});

  factory ApiException.fromDioError(DioException error) {
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return ApiException(message: 'Connection timeout');
      case DioExceptionType.badResponse:
        final response = error.response;
        if (response != null) {
          final message = response.data?['message'] ?? 'Something went wrong';
          return ApiException(
            message: message,
            statusCode: response.statusCode,
            data: response.data,
          );
        }
        return ApiException(message: 'Invalid response from server');
      case DioExceptionType.cancel:
        return ApiException(message: 'Request cancelled');
      default:
        return ApiException(message: 'Unexpected error occurred');
    }
  }

  @override
  String toString() => message;
}
