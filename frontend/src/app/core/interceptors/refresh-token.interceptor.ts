import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { AuthFacade } from '@app/features/auth/services/auth.facade';
import { CurrentUserStore } from '../auth/current-user.store';
import { RETRY_ONCE, SKIP_REFRESH } from '../auth/auth-request-context';

export const refreshTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const authFacade = inject(AuthFacade);
  const authStore = inject(CurrentUserStore);

  if (request.context.get(SKIP_REFRESH)) {
    return next(request);
  }

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      if (request.context.get(RETRY_ONCE)) {
        return throwError(() => error);
      }

      return from(authFacade.tryRefresh()).pipe(
        switchMap((refreshed) => {
          if (!refreshed) {
            return throwError(() => error);
          }

          const accessToken = authStore.accessToken();
          if (!accessToken) {
            return throwError(() => error);
          }

          return next(
            request.clone({
              context: request.context.set(RETRY_ONCE, true),
              setHeaders: {
                Authorization: `Bearer ${accessToken}`,
              },
            }),
          );
        }),
        catchError(() => throwError(() => error)),
      );
    }),
  );
};
