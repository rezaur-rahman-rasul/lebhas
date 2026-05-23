import 'package:flutter/material.dart';

class AppButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final bool isLoading;
  final bool isPrimary;
  final IconData? icon;

  const AppButton({
    super.key,
    required this.text,
    this.onPressed,
    this.isLoading = false,
    this.isPrimary = true,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    if (icon != null) {
      return ElevatedButton.icon(
        onPressed: isLoading ? null : onPressed,
        icon: isLoading 
            ? const SizedBox(
                width: 20, 
                height: 20, 
                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)
              )
            : Icon(icon),
        label: Text(text),
        style: _getButtonStyle(theme),
      );
    }

    return ElevatedButton(
      onPressed: isLoading ? null : onPressed,
      style: _getButtonStyle(theme),
      child: isLoading
          ? const SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
            )
          : Text(text),
    );
  }

  ButtonStyle _getButtonStyle(ThemeData theme) {
    if (isPrimary) {
      return theme.elevatedButtonTheme.style!;
    } else {
      return ElevatedButton.styleFrom(
        backgroundColor: Colors.transparent,
        foregroundColor: theme.colorScheme.primary,
        elevation: 0,
        side: BorderSide(color: theme.colorScheme.primary),
        minimumSize: const Size(double.infinity, 52),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
      );
    }
  }
}
