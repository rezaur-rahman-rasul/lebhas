import 'dart:async';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

Future<void> bootstrap(FutureOr<Widget> Function() builder) async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Add global error handling here if needed
  
  runApp(
    ProviderScope(
      child: await builder(),
    ),
  );
}
