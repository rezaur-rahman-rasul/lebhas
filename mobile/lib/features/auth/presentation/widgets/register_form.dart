import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_button.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_text_field.dart';
import 'package:lebhas_creative_maker/features/auth/state/auth_controller.dart';
import 'password_field.dart';

class RegisterForm extends ConsumerStatefulWidget {
  const RegisterForm({super.key});

  @override
  ConsumerState<RegisterForm> createState() => _RegisterFormState();
}

class _RegisterFormState extends ConsumerState<RegisterForm> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _onRegister() async {
    if (_formKey.currentState?.validate() ?? false) {
      final fullName = _nameController.text.trim();
      final names = fullName.split(' ');
      final firstName = names.isNotEmpty ? names.first : '';
      final lastName = names.length > 1 ? names.sublist(1).join(' ') : '';

      await ref.read(authProvider.notifier).register(
            firstName: firstName,
            lastName: lastName,
            email: _emailController.text.trim(),
            password: _passwordController.text,
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final isLoading = authState.maybeWhen(loading: () => true, orElse: () => false);
    final error = authState.maybeWhen(error: (msg) => msg, orElse: () => null);

    // Navigate to dashboard on successful registration
    ref.listen(authProvider, (previous, next) {
      next.maybeWhen(
        authenticated: (_) => context.go('/dashboard'),
        orElse: () {},
      );
    });

    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          AppTextField(
            controller: _nameController,
            label: context.l10n.fullName,
            prefixIcon: const Icon(Icons.person_outline),
            validator: (value) {
              if (value == null || value.isEmpty) return 'Name is required';
              if (!value.trim().contains(' ')) return 'Please enter both first and last name';
              return null;
            },
          ),
          const SizedBox(height: 16),
          AppTextField(
            controller: _emailController,
            label: context.l10n.email,
            prefixIcon: const Icon(Icons.email_outlined),
            keyboardType: TextInputType.emailAddress,
            validator: (value) {
              if (value == null || value.isEmpty) return context.l10n.emailRequired;
              if (!value.contains('@')) return context.l10n.invalidEmail;
              return null;
            },
          ),
          const SizedBox(height: 16),
          PasswordField(
            controller: _passwordController,
            label: context.l10n.password,
            validator: (value) {
              if (value == null || value.isEmpty) return context.l10n.passwordRequired;
              if (value.length < 6) return 'Password must be at least 6 characters';
              return null;
            },
          ),
          if (error != null) ...[
            const SizedBox(height: 16),
            Text(
              error,
              style: TextStyle(color: context.colorScheme.error, fontSize: 13),
              textAlign: TextAlign.center,
            ),
          ],
          const SizedBox(height: 32),
          AppButton(
            text: context.l10n.signup,
            isLoading: isLoading,
            onPressed: _onRegister,
          ),
        ],
      ),
    );
  }
}
