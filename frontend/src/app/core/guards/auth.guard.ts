import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { CurrentUserStore } from '../auth/current-user.store';
import { AuthFacade } from '@app/features/auth/services/auth.facade';

export const authGuard: CanActivateFn = async (_route, state) => {
  const authFacade = inject(AuthFacade);
  const authState = inject(CurrentUserStore);
  const router = inject(Router);

  await authFacade.initialize();

  if (authState.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: {
      returnUrl: state.url,
    },
  });
};
