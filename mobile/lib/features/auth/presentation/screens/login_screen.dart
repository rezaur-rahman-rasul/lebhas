import 'package:flutter/material.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/features/auth/presentation/widgets/login_form.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(context.l10n.login),
      ),
      body: const SingleChildScrollView(
        padding: EdgeInsets.all(24.0),
        child: LoginForm(),
      ),
    );
  }
}
