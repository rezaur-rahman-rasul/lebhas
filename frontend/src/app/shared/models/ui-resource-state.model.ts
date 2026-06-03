export type UiResourceState<T> =
  | { readonly status: 'idle' }
  | { readonly status: 'loading' }
  | { readonly status: 'success'; readonly data: T }
  | { readonly status: 'empty'; readonly message?: string }
  | {
      readonly status: 'error';
      readonly message: string;
      readonly code?: string;
      readonly retryable: boolean;
    };

export type UiResourceErrorKind =
  | 'validation'
  | 'forbidden'
  | 'endpoint-unavailable'
  | 'server'
  | 'network'
  | 'unknown';

export function uiResourceErrorMessage(kind: UiResourceErrorKind, fallback?: string): string {
  switch (kind) {
    case 'validation':
      return fallback || 'The request could not be validated.';
    case 'forbidden':
      return 'You do not have permission to view this area.';
    case 'endpoint-unavailable':
      return 'Configuration endpoint is not available yet.';
    case 'server':
      return fallback || 'Data could not load. Please retry.';
    case 'network':
      return 'Unable to reach the API gateway. Confirm the backend is running.';
    default:
      return fallback || 'Data could not load. Please retry.';
  }
}

export function uiResourceErrorKind(status: number): UiResourceErrorKind {
  if (status === 0) {
    return 'network';
  }
  if (status === 400) {
    return 'validation';
  }
  if (status === 403) {
    return 'forbidden';
  }
  if (status === 404) {
    return 'endpoint-unavailable';
  }
  if (status >= 500) {
    return 'server';
  }
  return 'unknown';
}
