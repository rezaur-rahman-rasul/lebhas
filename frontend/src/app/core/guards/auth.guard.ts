import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { getDefaultRouteForRole } from '../auth/auth-redirects';
import { CurrentUserStore } from '../auth/current-user.store';
import { AuthFacade } from '@app/features/auth/services/auth.facade';

export const authGuard: CanActivateFn = async (_route, state) => {
  const authFacade = inject(AuthFacade);
  const authState = inject(CurrentUserStore);
  const router = inject(Router);

  await authFacade.initialize();

  if (authState.isAuthenticated()) {
    const role = authState.currentRole();
    const path = state.url.split('?')[0]?.split('#')[0] ?? state.url;

    if (role === 'MASTER' && path !== '/audit-logs' && !path.startsWith('/master/')) {
      return router.createUrlTree([getDefaultRouteForRole(role)]);
    }

    if ((role === 'ADMIN' || role === 'CREW') && (path === '/master' || path.startsWith('/master/'))) {
      return router.createUrlTree([getDefaultRouteForRole(role)]);
    }

    return true;
  }

  return router.createUrlTree(['/'], {
    queryParams: {
      auth: 'login',
      returnUrl: state.url,
    },
  });
};
