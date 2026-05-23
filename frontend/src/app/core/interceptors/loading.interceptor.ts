import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { SKIP_GLOBAL_LOADING } from '../auth/auth-request-context';
import { LoadingStateService } from '../state/loading-state.service';

export const loadingInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.context.get(SKIP_GLOBAL_LOADING)) {
    return next(request);
  }

  const loading = inject(LoadingStateService);
  loading.start();

  return next(request).pipe(finalize(() => loading.stop()));
};
