import { hasPermission } from '@app/core/auth/permissions';
import { Permission, UserRole } from '@app/features/auth/models/user.models';

export interface PromptPermissionContext {
  readonly role: UserRole | null;
  readonly permissions: readonly Permission[];
}

export function isMasterSupportMode(role: UserRole | null): boolean {
  return role === 'MASTER';
}

export function isAdminRole(role: UserRole | null): boolean {
  return role === 'ADMIN';
}

/** Opens the project-scoped prompt builder surface. */
export function canUsePromptBuilder(context: PromptPermissionContext): boolean {
  if (isMasterSupportMode(context.role) || isAdminRole(context.role)) {
    return true;
  }

  return hasPermission(context.permissions, 'PROMPT_INTELLIGENCE_USE');
}

/** Triggers AI enhancement actions in the builder. */
export function canEnhancePrompt(context: PromptPermissionContext): boolean {
  return canUsePromptBuilder(context);
}

/** Creates, updates, and deletes workspace prompt templates. */
export function canManageTemplates(context: PromptPermissionContext): boolean {
  if (isMasterSupportMode(context.role) || isAdminRole(context.role)) {
    return true;
  }

  return hasPermission(context.permissions, 'PROMPT_TEMPLATE_MANAGE');
}

/** Reads workspace prompt templates (includes manage permission). */
export function canViewPromptTemplates(context: PromptPermissionContext): boolean {
  if (isMasterSupportMode(context.role) || isAdminRole(context.role)) {
    return true;
  }

  return (
    canManageTemplates(context) || hasPermission(context.permissions, 'PROMPT_TEMPLATE_VIEW')
  );
}

/** Reads project-scoped prompt history. */
export function canViewPromptHistory(context: PromptPermissionContext): boolean {
  if (isMasterSupportMode(context.role) || isAdminRole(context.role)) {
    return true;
  }

  return (
    hasPermission(context.permissions, 'PROMPT_HISTORY_VIEW') ||
    hasPermission(context.permissions, 'PROMPT_TEMPLATE_MANAGE')
  );
}

export function createPromptPermissionContext(
  role: UserRole | null,
  permissions: readonly Permission[],
): PromptPermissionContext {
  return { role, permissions };
}
