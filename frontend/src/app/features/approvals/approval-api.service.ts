import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import { ApprovalDecisionPayload, ApprovalItem } from './approval.models';

@Injectable({ providedIn: 'root' })
export class ApprovalApiService {
  private readonly api = inject(ApiService);

  async list(workspaceId: string): Promise<readonly ApprovalItem[]> {
    const response = await firstValueFrom(
      this.api.get<readonly ApprovalItem[]>(`/api/v1/workspaces/${workspaceId}/approvals`),
    );
    return unwrapApiResponse(response);
  }

  async decide(
    workspaceId: string,
    approvalId: string,
    payload: ApprovalDecisionPayload,
  ): Promise<ApprovalItem> {
    const response = await firstValueFrom(
      this.api.post<ApprovalItem, ApprovalDecisionPayload>(
        `/api/v1/workspaces/${workspaceId}/approvals/${approvalId}/decision`,
        payload,
      ),
    );
    return unwrapApiResponse(response);
  }
}
