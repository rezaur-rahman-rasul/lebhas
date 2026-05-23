import 'package:flutter/material.dart';
import '../widgets/public_app_bar.dart';
import '../widgets/hero_section.dart';
import '../widgets/platform_strip.dart';
import '../widgets/workflow_section.dart';
import '../widgets/feature_section.dart';
import '../widgets/bangladesh_section.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      appBar: PublicAppBar(),
      body: SingleChildScrollView(
        child: Column(
          children: [
            HeroSection(),
            PlatformStrip(),
            WorkflowSection(),
            FeatureSection(),
            BangladeshSection(),
            SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}
