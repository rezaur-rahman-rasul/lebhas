import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../features/auth/state/auth_controller.dart';
import '../features/public/presentation/screens/home_screen.dart';
import '../features/auth/presentation/screens/login_screen.dart';
import '../features/auth/presentation/screens/register_screen.dart';
import '../features/dashboard/presentation/screens/dashboard_screen.dart';
import '../features/brands/presentation/screens/brand_list_screen.dart';
import '../features/brands/presentation/screens/brand_details_screen.dart';
import '../features/brands/presentation/screens/brand_form_screen.dart';
import '../features/product_services/presentation/screens/product_service_list_screen.dart';
import '../features/product_services/presentation/screens/product_service_form_screen.dart';
import '../features/product_services/presentation/screens/product_service_details_screen.dart';
import '../features/projects/presentation/screens/project_list_screen.dart';
import '../features/projects/presentation/screens/project_form_screen.dart';
import '../features/projects/presentation/screens/project_details_screen.dart';
import '../features/assets/presentation/screens/asset_library_screen.dart';
import '../features/assets/presentation/screens/project_assets_screen.dart';
import '../features/assets/presentation/screens/asset_detail_screen.dart';
import '../shared/layouts/main_layout.dart';

final rootNavigatorKey = GlobalKey<NavigatorState>();
final shellNavigatorKey = GlobalKey<NavigatorState>();

final routerProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authProvider);

  return GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: '/',
    redirect: (context, state) {
      final isLoggedIn = authState.maybeWhen(
        authenticated: (_) => true,
        orElse: () => false,
      );

      final isLoggingIn = state.uri.toString() == '/login' || state.uri.toString() == '/register';
      final isPublic = state.uri.toString() == '/' || isLoggingIn;

      if (!isLoggedIn && !isPublic) {
        return '/';
      }

      if (isLoggedIn && isLoggingIn) {
        return '/dashboard';
      }

      return null;
    },
    routes: [
      GoRoute(
        path: '/',
        name: 'home',
        builder: (context, state) => const HomeScreen(),
      ),
      GoRoute(
        path: '/login',
        name: 'login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/register',
        name: 'register',
        builder: (context, state) => const RegisterScreen(),
      ),
      
      ShellRoute(
        navigatorKey: shellNavigatorKey,
        builder: (context, state, child) {
          return MainLayout(child: child);
        },
        routes: [
          GoRoute(
            path: '/dashboard',
            name: 'dashboard',
            builder: (context, state) => const DashboardScreen(),
          ),
          // Brands
          GoRoute(
            path: '/brands',
            name: 'brands',
            builder: (context, state) => const BrandListScreen(),
            routes: [
              GoRoute(
                path: 'new',
                name: 'brand-new',
                builder: (context, state) => const BrandFormScreen(),
              ),
              GoRoute(
                path: ':brandId',
                name: 'brand-details',
                builder: (context, state) => BrandDetailScreen(brandId: state.pathParameters['brandId']!),
              ),
              GoRoute(
                path: ':brandId/edit',
                name: 'brand-edit',
                builder: (context, state) => BrandFormScreen(brandId: state.pathParameters['brandId']),
              ),
            ],
          ),
          // Products
          GoRoute(
            path: '/product-services',
            name: 'product-services',
            builder: (context, state) => const ProductServiceListScreen(),
            routes: [
              GoRoute(
                path: 'new',
                name: 'product-new',
                builder: (context, state) {
                   final brandId = state.uri.queryParameters['brandId'];
                   return ProductServiceFormScreen(initialBrandId: brandId);
                },
              ),
              GoRoute(
                path: ':productId',
                name: 'product-details',
                builder: (context, state) => ProductServiceDetailScreen(productServiceId: state.pathParameters['productId']!),
              ),
              GoRoute(
                path: ':productId/edit',
                name: 'product-edit',
                builder: (context, state) => ProductServiceFormScreen(productServiceId: state.pathParameters['productId']),
              ),
            ],
          ),
          // Projects
          GoRoute(
            path: '/projects',
            name: 'projects',
            builder: (context, state) => const ProjectListScreen(),
            routes: [
              GoRoute(
                path: 'new',
                name: 'project-new',
                builder: (context, state) {
                  final productServiceId = state.uri.queryParameters['productServiceId'];
                  return ProjectFormScreen(initialProductServiceId: productServiceId);
                },
              ),
              GoRoute(
                path: ':projectId',
                name: 'project-details',
                builder: (context, state) => ProjectDetailScreen(projectId: state.pathParameters['projectId']!),
              ),
              GoRoute(
                path: ':projectId/edit',
                name: 'project-edit',
                builder: (context, state) => ProjectFormScreen(projectId: state.pathParameters['projectId']),
              ),
              // Scoped Assets
              GoRoute(
                path: ':projectId/assets',
                name: 'project-assets',
                builder: (context, state) => ProjectAssetsScreen(projectId: state.pathParameters['projectId']!),
              ),
            ],
          ),
          // Assets Global
          GoRoute(
            path: '/assets',
            name: 'assets',
            builder: (context, state) => const AssetLibraryScreen(),
            routes: [
              GoRoute(
                path: ':assetId',
                name: 'asset-details',
                builder: (context, state) => AssetDetailScreen(assetId: state.pathParameters['assetId']!),
              ),
            ],
          ),
          GoRoute(
            path: '/creative-versions',
            name: 'creative-versions',
            builder: (context, state) => const Center(child: Text('Creative Versions')),
          ),
          GoRoute(
            path: '/approvals',
            name: 'approvals',
            builder: (context, state) => const Center(child: Text('Approvals')),
          ),
        ],
      ),
    ],
  );
});
