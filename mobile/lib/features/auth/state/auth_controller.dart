import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../workspace/state/workspace_controller.dart';
import '../data/auth_repository.dart';
import '../domain/auth_models.dart';

final authProvider = StateNotifierProvider<AuthController, AuthState>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return AuthController(repository, ref);
});

class AuthController extends StateNotifier<AuthState> {
  final AuthRepository _repository;
  final Ref _ref;

  AuthController(this._repository, this._ref) : super(const AuthState.initial()) {
    checkAuth();
  }

  Future<void> checkAuth() async {
    state = const AuthState.loading();
    try {
      final user = await _repository.getCurrentUser();
      if (user != null) {
        state = AuthState.authenticated(user);
        // Load workspaces after auth check
        _ref.read(workspaceControllerProvider.notifier).loadWorkspaces();
      } else {
        state = const AuthState.unauthenticated();
      }
    } catch (e) {
      state = AuthState.error(e.toString());
    }
  }

  Future<void> login(String email, String password) async {
    state = const AuthState.loading();
    try {
      final authResponse = await _repository.login(LoginRequest(email: email, password: password));
      state = AuthState.authenticated(authResponse.user);
      // Load workspaces after successful login
      await _ref.read(workspaceControllerProvider.notifier).loadWorkspaces();
    } catch (e) {
      state = AuthState.error(e.toString());
    }
  }

  Future<void> register({
    required String firstName,
    required String lastName,
    required String email,
    required String password,
  }) async {
    state = const AuthState.loading();
    try {
      final authResponse = await _repository.register(
        RegisterRequest(
          firstName: firstName,
          lastName: lastName,
          email: email,
          password: password,
        ),
      );
      state = AuthState.authenticated(authResponse.user);
      // Load workspaces after successful registration
      await _ref.read(workspaceControllerProvider.notifier).loadWorkspaces();
    } catch (e) {
      state = AuthState.error(e.toString());
    }
  }

  Future<void> logout() async {
    await _repository.logout();
    state = const AuthState.unauthenticated();
    // Clear workspace state
    _ref.invalidate(workspaceControllerProvider);
  }
}
