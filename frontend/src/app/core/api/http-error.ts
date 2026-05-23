import { HttpErrorResponse } from '@angular/common/http';

import { ApiError, ApiResponse } from '@app/shared/models/api-response.model';

export interface NormalizedHttpError {
  readonly status: number;
  readonly message: string;
  readonly errors: readonly ApiError[];
}

export function normalizeHttpError(error: unknown): NormalizedHttpError {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiResponse<unknown>> | null;
    if (error.status === 0) {
      return {
        status: 0,
        message: 'Unable to reach the API gateway. Confirm the local backend is running and accessible.',
        errors: [],
      };
    }

    return {
      status: error.status,
      message: body?.message || error.message || 'Request failed',
      errors: body?.errors ?? [],
    };
  }

  return {
    status: 0,
    message: 'Unexpected application error',
    errors: [],
  };
}
