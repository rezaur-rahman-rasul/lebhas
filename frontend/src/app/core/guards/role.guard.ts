import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { UserRole } from '@app/features/auth/models/user.models';
import { CurrentUserStore } from '../auth/current-user.store';
import { AuthFacade } from '@app/features/auth/services/auth.facade';

export function roleGuard(allowedRoles: readonly UserRole[]): CanActivateFn {
  return async () => {
    const authFacade = inject(AuthFacade);
    const authState = inject(CurrentUserStore);
    const router = inject(Router);

    await authFacade.initialize();

    if (authState.hasAnyRole(allowedRoles)) {
      return true;
    }

    return router.createUrlTree(['/dashboard']);
  };
}
