export interface ApiError {
  readonly code: string;
  readonly field?: string;
  readonly message: string;
}

export interface ApiResponse<T> {
  readonly success: boolean;
  readonly message: string;
  readonly data: T;
  readonly errors: readonly ApiError[];
  readonly timestamp: string;
}
