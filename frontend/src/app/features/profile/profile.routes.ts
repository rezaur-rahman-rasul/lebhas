import { Routes } from '@angular/router';

export const PROFILE_ROUTES: Routes = [
  {
    path: 'profile',
    loadComponent: () => import('./page/profile-page').then((m) => m.ProfilePage),
  },
  {
    path: 'profile/edit',
    loadComponent: () =>
      import('./edit-profile/edit-profile-form').then((m) => m.EditProfileFormPage),
  },
  {
    path: 'profile/change-password',
    loadComponent: () =>
      import('./change-password/change-password-form').then((m) => m.ChangePasswordFormPage),
  },
  {
    path: 'profile/settings',
    loadComponent: () => import('./settings/account-settings').then((m) => m.AccountSettingsPage),
  },
  {
    path: 'profile/security',
    loadComponent: () =>
      import('./security/security-activity').then((m) => m.SecurityActivityPage),
  },
  {
    path: 'profile/sessions',
    loadComponent: () => import('./sessions/user-sessions').then((m) => m.UserSessionsPage),
  },
];
