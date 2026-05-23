import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { environment } from '@env/environment';
import { CurrentUserStore } from '../auth/current-user.store';

export const tenantInterceptor: HttpInterceptorFn = (request, next) => {
  const workspaceId = inject(CurrentUserStore).activeWorkspaceId();

  if (!workspaceId) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        [environment.workspaceHeaderName]: workspaceId,
      },
    }),
  );
};
