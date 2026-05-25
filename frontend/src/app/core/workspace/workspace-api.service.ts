import { HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import { WorkspaceContext, WorkspaceSummary } from './workspace.models';

@Injectable({ providedIn: 'root' })
export class WorkspaceApiService {
  private readonly api = inject(ApiService);

  async getAccessibleWorkspaces(): Promise<readonly WorkspaceSummary[]> {
    const response = await firstValueFrom(this.api.get<WorkspaceSummary[]>('/api/v1/workspaces/me'));
    return unwrapApiResponse(response);
  }

  async getWorkspaceContext(workspaceId: string): Promise<WorkspaceContext> {
    const response = await firstValueFrom(
      this.api.get<WorkspaceContext>(`/api/v1/workspaces/${workspaceId}/context`, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      }),
    );
    return unwrapApiResponse(response);
  }
}

