import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import '../widgets/register_form.dart';

class RegisterScreen extends StatelessWidget {
  const RegisterScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(context.l10n.createAccount),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              context.l10n.joinLebhas,
              style: context.textTheme.displayMedium,
            ),
            const SizedBox(height: 8),
            Text(
              context.l10n.registerSubtitle,
              style: context.textTheme.bodyMedium,
            ),
            const SizedBox(height: 32),
            const RegisterForm(),
            const SizedBox(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(context.l10n.alreadyHaveAccount),
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: Text(context.l10n.login),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
