import { HttpInterceptorFn } from '@angular/common/http';

import { environment } from '@env/environment';
import { createCorrelationId } from '@app/shared/utils/create-correlation-id';
import { SKIP_APP_HEADERS } from '../auth/auth-request-context';

export const correlationIdInterceptor: HttpInterceptorFn = (request, next) =>
  request.context.get(SKIP_APP_HEADERS)
    ? next(request)
    : next(
        request.clone({
          setHeaders: {
            [environment.correlationIdHeaderName]: createCorrelationId(),
          },
        }),
      );
