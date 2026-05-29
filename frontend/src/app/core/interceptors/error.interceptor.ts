import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { normalizeHttpError } from '../api/http-error';
import { SKIP_ERROR_TOAST } from '../auth/auth-request-context';
import { NotificationStateService } from '../state/notification-state.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(NotificationStateService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (request.context.get(SKIP_ERROR_TOAST)) {
        return throwError(() => error);
      }

      const normalized = normalizeHttpError(error);
      if (normalized.status !== 401 && shouldToastRequestFailure(request.method)) {
        notifications.error(
          requestFailureTitle(request.url),
          requestFailureMessage(normalized.status, request.url, normalized.message),
          `${router.url}|${normalized.status}|${requestFailureTitle(request.url)}`,
        );
      }
      return throwError(() => error);
    }),
  );
};

function shouldToastRequestFailure(method: string): boolean {
  return ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method.toUpperCase());
}

function requestFailureTitle(url: string): string {
  if (url.includes('ai-monitoring') || url.includes('/ai/')) {
    return 'AI monitoring data could not load';
  }

  if (url.includes('usage')) {
    return 'Usage data could not load';
  }

  if (url.includes('payment') || url.includes('invoice')) {
    return 'Payment data could not load';
  }

  if (url.includes('monitoring') || url.includes('health')) {
    return 'System health could not load';
  }

  if (url.includes('audit')) {
    return 'Audit logs could not load';
  }

  return 'Request failed';
}

function requestFailureMessage(status: number, url: string, fallback: string): string {
  if (status === 403) {
    return 'You do not have permission to view this data.';
  }

  if (status === 404) {
    return 'This data is not available yet. Please try again later.';
  }

  if (status === 0) {
    return 'We could not reach the backend. Please check that services are running.';
  }

  if (status >= 500) {
    return 'We could not load this data. Please try again.';
  }

  return fallback || requestFailureTitle(url);
}
