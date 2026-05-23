import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiResponse } from '@app/shared/models/api-response.model';
import { WorkspaceSummary } from './workspace.models';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly api = inject(ApiService);

  async getAccessibleWorkspaces(): Promise<readonly WorkspaceSummary[]> {
    const response = await firstValueFrom(this.api.get<WorkspaceSummary[]>('/api/v1/workspaces/me'));
    return this.unwrap(response);
  }

  private unwrap<T>(response: ApiResponse<T>): T {
    return response.data;
  }
}

