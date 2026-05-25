import { ApiResponse } from '@app/shared/models/api-response.model';

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  return response.data;
}
