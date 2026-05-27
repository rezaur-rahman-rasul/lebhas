import { HttpClient, HttpContext, HttpEventType, HttpHeaders, HttpRequest } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import {
  SKIP_APP_HEADERS,
  SKIP_AUTH,
  SKIP_ERROR_TOAST,
  SKIP_GLOBAL_LOADING,
  SKIP_REFRESH,
} from '@app/core/auth/auth-request-context';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  ChangePasswordRequest,
  ConfirmProfileImageUploadRequest,
  ProfileImageUploadUrlRequest,
  ProfileImageUploadUrlResponse,
  SecurityActivityView,
  SupportUserProfileView,
  UpdateAccountSettingsRequest,
  UpdateProfileRequest,
  UserAccountSettings,
  UserProfileView,
  UserSessionView,
} from '../models/profile.models';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  getMyProfile(): Promise<UserProfileView> {
    return this.get<UserProfileView>('/api/v1/profile/me');
  }

  updateMyProfile(payload: UpdateProfileRequest): Promise<UserProfileView> {
    return this.put<UserProfileView, UpdateProfileRequest>('/api/v1/profile/me', payload);
  }

  updateMySettings(payload: UpdateAccountSettingsRequest): Promise<UserAccountSettings> {
    return this.put<UserAccountSettings, UpdateAccountSettingsRequest>(
      '/api/v1/profile/me/settings',
      payload,
    );
  }

  changePassword(payload: ChangePasswordRequest): Promise<void> {
    return this.post<void, ChangePasswordRequest>('/api/v1/profile/me/change-password', payload);
  }

  requestProfileImageUploadUrl(
    payload: ProfileImageUploadUrlRequest,
  ): Promise<ProfileImageUploadUrlResponse> {
    return this.post<ProfileImageUploadUrlResponse, ProfileImageUploadUrlRequest>(
      '/api/v1/profile/me/profile-image/upload-url',
      payload,
    );
  }

  confirmProfileImageUpload(payload: ConfirmProfileImageUploadRequest): Promise<UserProfileView> {
    return this.post<UserProfileView, ConfirmProfileImageUploadRequest>(
      '/api/v1/profile/me/profile-image/confirm',
      payload,
    );
  }

  uploadProfileImageToSignedUrl(
    uploadUrl: string,
    file: File,
    onProgress: (progress: number) => void,
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = new HttpRequest('PUT', uploadUrl, file, {
        reportProgress: true,
        context: new HttpContext()
          .set(SKIP_AUTH, true)
          .set(SKIP_REFRESH, true)
          .set(SKIP_ERROR_TOAST, true)
          .set(SKIP_GLOBAL_LOADING, true)
          .set(SKIP_APP_HEADERS, true),
        headers: new HttpHeaders({ 'Content-Type': file.type }),
      });

      const subscription = this.http.request(request).subscribe({
        next: (event) => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            onProgress(Math.round((event.loaded / event.total) * 100));
          }

          if (event.type === HttpEventType.Response) {
            onProgress(100);
            resolve();
          }
        },
        error: (error: unknown) => {
          subscription.unsubscribe();
          reject(error);
        },
        complete: () => subscription.unsubscribe(),
      });
    });
  }

  removeProfileImage(): Promise<UserProfileView> {
    return this.delete<UserProfileView>('/api/v1/profile/me/profile-image');
  }

  getSecurityActivity(): Promise<readonly SecurityActivityView[]> {
    return this.get<readonly SecurityActivityView[]>('/api/v1/profile/me/security-activity');
  }

  getSessions(): Promise<readonly UserSessionView[]> {
    return this.get<readonly UserSessionView[]>('/api/v1/profile/me/sessions');
  }

  revokeSession(sessionId: string): Promise<void> {
    return this.post<void, Record<string, never>>(
      `/api/v1/profile/me/sessions/${this.path(sessionId)}/revoke`,
      {},
    );
  }

  getMasterUserProfile(userId: string): Promise<SupportUserProfileView> {
    return this.get<SupportUserProfileView>(`/api/v1/master/users/${this.path(userId)}/profile`);
  }

  getMasterUserSecurityActivity(userId: string): Promise<readonly SecurityActivityView[]> {
    return this.get<readonly SecurityActivityView[]>(
      `/api/v1/master/users/${this.path(userId)}/security-activity`,
    );
  }

  private async get<T>(path: string): Promise<T> {
    const response = await firstValueFrom(this.api.get<T>(path));
    return unwrapApiResponse(response);
  }

  private async post<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.post<T, TBody>(path, body));
    return unwrapApiResponse(response);
  }

  private async put<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(this.api.put<T, TBody>(path, body));
    return unwrapApiResponse(response);
  }

  private async delete<T>(path: string): Promise<T> {
    const response = await firstValueFrom(this.api.delete<T>(path));
    return unwrapApiResponse(response);
  }

  private path(value: string): string {
    return encodeURIComponent(value);
  }
}
