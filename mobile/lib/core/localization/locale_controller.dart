import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../storage/secure_storage_service.dart';
import '../constants/storage_keys.dart';

final localeControllerProvider = StateNotifierProvider<LocaleController, Locale>((ref) {
  final storage = ref.watch(secureStorageServiceProvider);
  return LocaleController(storage);
});

class LocaleController extends StateNotifier<Locale> {
  final SecureStorageService _storage;

  LocaleController(this._storage) : super(const Locale('en')) {
    _loadLocale();
  }

  Future<void> _loadLocale() async {
    final languageCode = await _storage.read(StorageKeys.locale);
    if (languageCode == 'bn') {
      state = const Locale('bn');
    } else {
      state = const Locale('en');
    }
  }

  Future<void> setLocale(Locale locale) async {
    state = locale;
    await _storage.write(StorageKeys.locale, locale.languageCode);
  }

  void toggleLocale() {
    if (state.languageCode == 'en') {
      setLocale(const Locale('bn'));
    } else {
      setLocale(const Locale('en'));
    }
  }
}
