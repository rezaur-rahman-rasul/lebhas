import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { CurrentUserStore } from '../auth/current-user.store';
import { defaultAuthenticatedRoute } from '../auth/permissions';
import { AuthFacade } from '@app/features/auth/services/auth.facade';

export const guestGuard: CanActivateFn = async () => {
  const authFacade = inject(AuthFacade);
  const authState = inject(CurrentUserStore);
  const router = inject(Router);

  await authFacade.initialize();

  if (!authState.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree([defaultAuthenticatedRoute()]);
};
