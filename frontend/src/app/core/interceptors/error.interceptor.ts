import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { normalizeHttpError } from '../api/http-error';
import { SKIP_ERROR_TOAST } from '../auth/auth-request-context';
import { NotificationStateService } from '../state/notification-state.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(NotificationStateService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (request.context.get(SKIP_ERROR_TOAST)) {
        return throwError(() => error);
      }

      const normalized = normalizeHttpError(error);
      if (normalized.status !== 401) {
        notifications.error('Request failed', normalized.message);
      }
      return throwError(() => error);
    }),
  );
};
