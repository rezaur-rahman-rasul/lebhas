import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/app/app.dart';
import 'package:lebhas_creative_maker/features/public/presentation/screens/home_screen.dart';
import 'package:lebhas_creative_maker/features/auth/presentation/widgets/login_bottom_sheet.dart';

void main() {
  testWidgets('App launches Home Screen and opens Login Bottom Sheet', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(
      const ProviderScope(
        child: LebhasApp(),
      ),
    );

    // Verify that the Home Screen is shown.
    expect(find.byType(HomeScreen), findsOneWidget);

    // Verify that the app starts in Dark Mode (checking scaffold background color)
    final scaffold = tester.widget<Scaffold>(find.byType(Scaffold).first);
    expect(scaffold.backgroundColor, const Color(0xFF0F172A)); // AppColors.darkBackground

    // Find and tap the Login button in the App Bar.
    final loginButton = find.text('Login');
    expect(loginButton, findsOneWidget);
    await tester.tap(loginButton);
    await tester.pumpAndSettle();

    // Verify that the Login Bottom Sheet is displayed.
    expect(find.byType(LoginBottomSheet), findsOneWidget);
    expect(find.text('Welcome Back'), findsOneWidget);
  });

  testWidgets('Theme toggle switches between Dark and Light mode', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: LebhasApp(),
      ),
    );

    // Start in Dark Mode
    var scaffold = tester.widget<Scaffold>(find.byType(Scaffold).first);
    expect(scaffold.backgroundColor, const Color(0xFF0F172A));

    // Tap theme toggle (Icons.light_mode shows when in dark mode)
    final themeToggle = find.byIcon(Icons.light_mode);
    expect(themeToggle, findsOneWidget);
    await tester.tap(themeToggle);
    await tester.pumpAndSettle();

    // Should now be in Light Mode
    scaffold = tester.widget<Scaffold>(find.byType(Scaffold).first);
    expect(scaffold.backgroundColor, const Color(0xFFF8FAFC)); // AppColors.lightBackground
  });
}
