import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lebhas_creative_maker/features/workspace/state/workspace_controller.dart';

class PermissionHelpers {
  final List<String> permissions;
  final String role;

  PermissionHelpers({required this.permissions, required this.role});

  // Role Checks
  bool get isMaster => role == 'MASTER';
  bool get isAdmin => role == 'ADMIN' || isMaster;
  bool get isCrew => role == 'CREW';

  // Workspace Management
  bool get canManageWorkspaces => isMaster;
  bool get canManageUsers => isAdmin;

  // Brand Management
  bool get canManageBrands => isAdmin || permissions.contains('MANAGE_BRANDS');
  bool get canViewBrands => true;

  // Product/Service Management
  bool get canManageProducts => isAdmin || permissions.contains('MANAGE_PRODUCTS');
  bool get canViewProducts => true;

  // Project Management
  bool get canCreateProjects => isAdmin || permissions.contains('CREATE_PROJECTS');
  bool get canUpdateProjects => isAdmin || permissions.contains('UPDATE_PROJECTS');
  bool get canDeleteProjects => isAdmin || permissions.contains('DELETE_PROJECTS');
  bool get canViewProjects => true;

  // Asset Management (Day 3)
  bool get canViewAssets => true;
  bool get canUploadAssets => isAdmin || permissions.contains('UPLOAD_ASSETS');
  bool get canDownloadAssets => isAdmin || permissions.contains('DOWNLOAD_ASSETS') || (isCrew && permissions.contains('CREW_DOWNLOAD'));
  bool get canDeleteAssets => isAdmin || permissions.contains('DELETE_ASSETS');

  // Creative Workflow
  bool get canCreateCreativeRequests => isAdmin || isCrew || permissions.contains('CREATE_CREATIVE_REQUESTS');
  bool get canEditCreativeBeforeApproval => isAdmin || permissions.contains('EDIT_CREATIVE');
  bool get canApproveCreative => isAdmin || permissions.contains('APPROVE_CREATIVE');

  // Admin/Master Support
  bool get canEnterSupportMode => isMaster;

  // Billing
  bool get canViewBilling => isAdmin;
}

final permissionsProvider = Provider<PermissionHelpers>((ref) {
  final context = ref.watch(workspaceContextProvider);
  return PermissionHelpers(
    permissions: context?.permissions ?? [],
    role: context?.role ?? 'CREW',
  );
});
