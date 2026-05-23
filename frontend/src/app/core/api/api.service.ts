import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '@env/environment';
import { ApiResponse } from '@app/shared/models/api-response.model';
import { joinUrl } from '@app/shared/utils/join-url';

type QueryValue = string | number | boolean | readonly (string | number | boolean)[];
type QueryParams = Record<string, QueryValue | null | undefined>;

interface RequestOptions {
  readonly params?: QueryParams;
  readonly context?: HttpContext;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  get<T>(path: string, options?: QueryParams | RequestOptions) {
    const requestOptions = this.normalizeOptions(options);
    return this.http.get<ApiResponse<T>>(this.url(path), {
      params: this.params(requestOptions.params),
      context: requestOptions.context,
    });
  }

  post<T, TBody = unknown>(path: string, body: TBody, options?: RequestOptions) {
    return this.http.post<ApiResponse<T>>(this.url(path), body, {
      context: options?.context,
    });
  }

  put<T, TBody = unknown>(path: string, body: TBody, options?: RequestOptions) {
    return this.http.put<ApiResponse<T>>(this.url(path), body, {
      context: options?.context,
    });
  }

  patch<T, TBody = unknown>(path: string, body: TBody, options?: RequestOptions) {
    return this.http.patch<ApiResponse<T>>(this.url(path), body, {
      context: options?.context,
    });
  }

  delete<T>(path: string, options?: QueryParams | RequestOptions) {
    const requestOptions = this.normalizeOptions(options);
    return this.http.delete<ApiResponse<T>>(this.url(path), {
      params: this.params(requestOptions.params),
      context: requestOptions.context,
    });
  }

  private url(path: string): string {
    return joinUrl(this.baseUrl, path);
  }

  private params(params?: QueryParams): HttpParams {
    let httpParams = new HttpParams();

    for (const [key, value] of Object.entries(params ?? {})) {
      if (value === null || value === undefined) {
        continue;
      }

      if (Array.isArray(value)) {
        for (const item of value) {
          httpParams = httpParams.append(key, String(item));
        }
      } else {
        httpParams = httpParams.set(key, String(value));
      }
    }

    return httpParams;
  }

  private normalizeOptions(options?: QueryParams | RequestOptions): RequestOptions {
    if (!options) {
      return {};
    }

    if ('params' in options || 'context' in options) {
      return options as RequestOptions;
    }

    return { params: options as QueryParams };
  }
}
