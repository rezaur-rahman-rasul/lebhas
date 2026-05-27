import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { environment } from '@env/environment';
import { CurrentUserStore } from '../auth/current-user.store';
import { SKIP_APP_HEADERS } from '../auth/auth-request-context';

export const tenantInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.context.get(SKIP_APP_HEADERS)) {
    return next(request);
  }

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
