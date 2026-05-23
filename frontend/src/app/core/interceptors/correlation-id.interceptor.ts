import { HttpInterceptorFn } from '@angular/common/http';

import { environment } from '@env/environment';
import { createCorrelationId } from '@app/shared/utils/create-correlation-id';

export const correlationIdInterceptor: HttpInterceptorFn = (request, next) =>
  next(
    request.clone({
      setHeaders: {
        [environment.correlationIdHeaderName]: createCorrelationId(),
      },
    }),
  );
