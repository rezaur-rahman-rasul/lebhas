import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { CurrentUserStore } from '../auth/current-user.store';
import { SKIP_AUTH } from '../auth/auth-request-context';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.context.get(SKIP_AUTH)) {
    return next(request);
  }

  const token = inject(CurrentUserStore).accessToken();

  if (!token) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};
