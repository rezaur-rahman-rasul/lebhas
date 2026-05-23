import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_button.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_text_field.dart';
import 'package:lebhas_creative_maker/features/auth/state/auth_controller.dart';
import 'password_field.dart';

class LoginForm extends ConsumerStatefulWidget {
  const LoginForm({super.key});

  @override
  ConsumerState<LoginForm> createState() => _LoginFormState();
}

class _LoginFormState extends ConsumerState<LoginForm> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _onLogin() async {
    if (_formKey.currentState?.validate() ?? false) {
      await ref.read(authProvider.notifier).login(
            _emailController.text.trim(),
            _passwordController.text,
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    
    // Use the extensions to get l10n
    final l10n = context.l10n;

    final isLoading = authState.maybeWhen(
      loading: () => true, 
      orElse: () => false,
    );
    final error = authState.maybeWhen(
      error: (msg) => msg, 
      orElse: () => null,
    );

    // Navigate to dashboard on successful login
    ref.listen(authProvider, (previous, next) {
      next.maybeWhen(
        authenticated: (_) {
          if (context.canPop()) context.pop(); // Close bottom sheet if open
          context.go('/dashboard');
        },
        orElse: () {},
      );
    });

    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          AppTextField(
            controller: _emailController,
            label: l10n.email,
            hint: 'your@email.com',
            prefixIcon: const Icon(Icons.email_outlined),
            keyboardType: TextInputType.emailAddress,
            validator: (value) {
              if (value == null || value.isEmpty) return l10n.emailRequired;
              if (!value.contains('@')) return l10n.invalidEmail;
              return null;
            },
          ),
          const SizedBox(height: 20),
          PasswordField(
            controller: _passwordController,
            label: l10n.password,
            validator: (value) {
              if (value == null || value.isEmpty) return l10n.passwordRequired;
              return null;
            },
          ),
          if (error != null) ...[
            const SizedBox(height: 12),
            Text(
              error,
              style: TextStyle(color: Theme.of(context).colorScheme.error, fontSize: 13),
              textAlign: TextAlign.center,
            ),
          ],
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () {}, // Forgot password placeholder
              child: Text(l10n.forgotPassword),
            ),
          ),
          const SizedBox(height: 24),
          AppButton(
            text: l10n.login,
            isLoading: isLoading,
            onPressed: _onLogin,
          ),
        ],
      ),
    );
  }
}
