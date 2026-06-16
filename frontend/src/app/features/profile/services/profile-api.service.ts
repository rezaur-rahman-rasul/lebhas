import { HttpClient, HttpContext, HttpEventType, HttpHeaders, HttpRequest } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
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
    return this.get<UserProfileView>(ApiEndpoints.profile.me);
  }

  updateMyProfile(payload: UpdateProfileRequest): Promise<UserProfileView> {
    return this.put<UserProfileView, UpdateProfileRequest>(ApiEndpoints.profile.me, payload);
  }

  async updateMyProfileWithImage(
    payload: UpdateProfileRequest,
    profileImage: File,
  ): Promise<UserProfileView> {
    const formData = new FormData();
    this.appendProfileField(formData, 'firstName', payload.firstName);
    this.appendProfileField(formData, 'lastName', payload.lastName);
    this.appendProfileField(formData, 'displayName', payload.displayName);
    this.appendProfileField(formData, 'phoneNumber', payload.phoneNumber);
    this.appendProfileField(formData, 'jobTitle', payload.jobTitle);
    this.appendProfileField(formData, 'timezone', payload.timezone);
    this.appendProfileField(formData, 'locale', payload.locale);
    formData.append('profileImage', profileImage, profileImage.name);

    const response = await firstValueFrom(
      this.api.put<UserProfileView, FormData>(
        ApiEndpoints.profile.me,
        formData,
        { context: this.profileRequestContext() },
      ),
    );
    return unwrapApiResponse(response);
  }

  updateMySettings(payload: UpdateAccountSettingsRequest): Promise<UserAccountSettings> {
    return this.put<UserAccountSettings, UpdateAccountSettingsRequest>(
      ApiEndpoints.profile.accountSettings,
      payload,
    );
  }

  changePassword(payload: ChangePasswordRequest): Promise<void> {
    return this.post<void, ChangePasswordRequest>(ApiEndpoints.profile.changePassword, payload);
  }

  requestProfileImageUploadUrl(
    payload: ProfileImageUploadUrlRequest,
  ): Promise<ProfileImageUploadUrlResponse> {
    return this.post<ProfileImageUploadUrlResponse, ProfileImageUploadUrlRequest>(
      ApiEndpoints.profile.profileImageUploadUrl,
      payload,
    );
  }

  confirmProfileImageUpload(payload: ConfirmProfileImageUploadRequest): Promise<UserProfileView> {
    return this.post<UserProfileView, ConfirmProfileImageUploadRequest>(
      ApiEndpoints.profile.profileImageConfirm,
      payload,
    );
  }

  async uploadProfileImage(file: File): Promise<UserProfileView> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await firstValueFrom(
      this.api.post<UserProfileView, FormData>(
        ApiEndpoints.profile.profileImageUpload,
        formData,
        { context: this.profileRequestContext() },
      ),
    );
    return unwrapApiResponse(response);
  }

  uploadProfileImageToSignedUrl(
    uploadUrl: string,
    file: File,
    method: string,
    onProgress: (progress: number) => void,
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = new HttpRequest(method || 'PUT', uploadUrl, file, {
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
    return this.delete<UserProfileView>(ApiEndpoints.profile.profileImage);
  }

  getSecurityActivity(): Promise<readonly SecurityActivityView[]> {
    return this.get<readonly SecurityActivityView[]>(ApiEndpoints.profile.securityActivity);
  }

  getSessions(): Promise<readonly UserSessionView[]> {
    return this.get<readonly UserSessionView[]>(ApiEndpoints.profile.sessions);
  }

  getRewardStatus(): Promise<Readonly<Record<string, boolean>>> {
    return this.get<Readonly<Record<string, boolean>>>(ApiEndpoints.profile.rewards);
  }

  updateRewardEmail(email: string): Promise<ProfileRewardResult> {
    return this.post<ProfileRewardResult, { readonly email: string }>(ApiEndpoints.profile.emailReward, { email });
  }

  connectFacebook(profileUrl: string): Promise<ProfileRewardResult> {
    return this.post<ProfileRewardResult, { readonly profileUrl: string }>(ApiEndpoints.profile.facebookConnection, { profileUrl });
  }

  connectInstagram(profileUrl: string): Promise<ProfileRewardResult> {
    return this.post<ProfileRewardResult, { readonly profileUrl: string }>(ApiEndpoints.profile.instagramConnection, { profileUrl });
  }

  revokeSession(sessionId: string): Promise<void> {
    return this.delete<void>(ApiEndpoints.profile.session(sessionId));
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
    const response = await firstValueFrom(this.api.get<T>(path, { context: this.profileRequestContext() }));
    return unwrapApiResponse(response);
  }

  private async post<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(
      this.api.post<T, TBody>(path, body, { context: this.profileRequestContext() }),
    );
    return unwrapApiResponse(response);
  }

  private async put<T, TBody>(path: string, body: TBody): Promise<T> {
    const response = await firstValueFrom(
      this.api.put<T, TBody>(path, body, { context: this.profileRequestContext() }),
    );
    return unwrapApiResponse(response);
  }

  private async delete<T>(path: string): Promise<T> {
    const response = await firstValueFrom(
      this.api.delete<T>(path, { context: this.profileRequestContext() }),
    );
    return unwrapApiResponse(response);
  }

  private profileRequestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }

  private appendProfileField(
    formData: FormData,
    key: keyof UpdateProfileRequest,
    value: string | null | undefined,
  ): void {
    if (value !== null && value !== undefined) {
      formData.append(key, value);
    }
  }

  private path(value: string): string {
    return encodeURIComponent(value);
  }
}

export interface ProfileRewardResult {
  readonly rewardType: string;
  readonly granted: boolean;
  readonly creditsGranted: number;
  readonly alreadyClaimedOrCompleted: boolean;
}
