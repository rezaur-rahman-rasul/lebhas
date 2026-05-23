import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { AuthFacade } from '@app/features/auth/services/auth.facade';

export const workspaceGuard: CanActivateFn = async () => {
  const authFacade = inject(AuthFacade);
  const authStore = inject(CurrentUserStore);
  const notifications = inject(NotificationStateService);
  const router = inject(Router);

  await authFacade.initialize();

  if (authStore.currentRole() === 'MASTER') {
    return true;
  }

  if (authStore.activeWorkspaceId()) {
    return true;
  }

  notifications.info(
    'Workspace required',
    'Select a workspace before opening this area.',
  );

  return router.createUrlTree(['/dashboard']);
};
