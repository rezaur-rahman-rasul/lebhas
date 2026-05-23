import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../storage/secure_storage_service.dart';
import '../constants/storage_keys.dart';

final themeControllerProvider = StateNotifierProvider<ThemeController, ThemeMode>((ref) {
  final storage = ref.watch(secureStorageServiceProvider);
  return ThemeController(storage);
});

class ThemeController extends StateNotifier<ThemeMode> {
  final SecureStorageService _storage;

  ThemeController(this._storage) : super(ThemeMode.dark) {
    _loadTheme();
  }

  Future<void> _loadTheme() async {
    final theme = await _storage.read(StorageKeys.themeMode);
    if (theme == 'light') {
      state = ThemeMode.light;
    } else {
      state = ThemeMode.dark;
    }
  }

  Future<void> toggleTheme() async {
    if (state == ThemeMode.dark) {
      state = ThemeMode.light;
      await _storage.write(StorageKeys.themeMode, 'light');
    } else {
      state = ThemeMode.dark;
      await _storage.write(StorageKeys.themeMode, 'dark');
    }
  }
}
