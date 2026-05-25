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
      message: friendlyHttpMessage(error.status, body, error.message),
      errors: body?.errors ?? [],
    };
  }

  return {
    status: 0,
    message: 'Unexpected application error',
    errors: [],
  };
}

function friendlyHttpMessage(
  status: number,
  body: Partial<ApiResponse<unknown>> | null,
  fallbackMessage: string,
): string {
  if (status === 403) {
    return 'You do not have permission to perform this action.';
  }

  const rawMessage = body?.message || fallbackMessage || 'Request failed';
  const errorText = [rawMessage, ...(body?.errors ?? []).map((error) => `${error.code ?? ''} ${error.message}`)]
    .join(' ')
    .toLowerCase();

  if (errorText.includes('quota') || errorText.includes('storage full')) {
    return 'Your workspace storage is full. Please remove old files or upgrade your package.';
  }

  if (errorText.includes('plan limit') || errorText.includes('limit exceeded')) {
    const explicitLimit = errorText.match(/\b\d+\b/)?.[0] ?? 'the allowed number of';
    return `Your current package allows only ${explicitLimit} creative version(s) for this request.`;
  }

  return rawMessage;
}
