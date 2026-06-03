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

  if (error instanceof Error && error.message.trim()) {
    if (error.name === 'TimeoutError' || error.message.toLowerCase().includes('timeout')) {
      return {
        status: 0,
        message: 'The request timed out. Please try again.',
        errors: [],
      };
    }
    return {
      status: 0,
      message: error.message,
      errors: [],
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
  const hasMissingStorageObject = (body?.errors ?? []).some((error) => error.code === 'ASSET_STORAGE_OBJECT_MISSING');
  if (hasMissingStorageObject) {
    return body?.message || 'Asset file is not available in storage. The stale asset was removed from the library.';
  }

  if (status === 403) {
    return 'You do not have permission to perform this action.';
  }

  if (status === 404) {
    return 'This data is not available yet. Please try again later.';
  }

  const rawMessage = body?.message || fallbackMessage || 'Request failed';
  const detailMessages = (body?.errors ?? [])
    .map((error) => [error.field, error.message].filter(Boolean).join(': '))
    .filter((message) => message.trim().length > 0);
  const errorText = [rawMessage, ...detailMessages, ...(body?.errors ?? []).map((error) => `${error.code ?? ''} ${error.message}`)]
    .join(' ')
    .toLowerCase();

  if (
    status === 400 &&
    detailMessages.length > 0 &&
    (rawMessage.toLowerCase().includes('validation failed') || rawMessage.toLowerCase().includes('validation'))
  ) {
    return detailMessages.join(' ');
  }

  if (errorText.includes('quota') || errorText.includes('storage full')) {
    return 'Your workspace storage is full. Please remove old files or upgrade your package.';
  }

  if (
    errorText.includes('planfeaturepolicy.maxgeneratedversionsperrequest') ||
    errorText.includes('maxgeneratedversionsperrequest') ||
    errorText.includes('plan limit') ||
    errorText.includes('limit exceeded')
  ) {
    const explicitLimit = errorText.match(/\b\d+\b/)?.[0] ?? 'the allowed number of';
    return `Your current package allows only ${explicitLimit} creative version(s) for this request.`;
  }

  if (
    (status >= 500 && !body?.message) ||
    errorText.includes('unexpected server error') ||
    errorText.includes('internal server error')
  ) {
    return 'We could not load this data. Please try again.';
  }

  return rawMessage;
}
