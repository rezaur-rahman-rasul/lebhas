import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_empty_state.dart';

class CreativeRequestListScreen extends ConsumerWidget {
  const CreativeRequestListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('Creative Requests')),
      body: const AppEmptyState(
        title: 'No Creative Requests',
        message: 'Select a project to start creating requests.',
        icon: Icons.add_photo_alternate_outlined,
      ),
    );
  }
}
