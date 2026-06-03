import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import { ApprovalDecisionPayload, ApprovalItem } from './approval.models';

@Injectable({ providedIn: 'root' })
export class ApprovalApiService {
  private readonly api = inject(ApiService);

  async list(workspaceId: string): Promise<readonly ApprovalItem[]> {
    const response = await firstValueFrom(
      this.api.get<readonly ApprovalItem[]>(ApiEndpoints.generatedVersions.approvals(workspaceId)),
    );
    return unwrapApiResponse(response);
  }

  async decide(
    workspaceId: string,
    generatedVersionId: string,
    payload: ApprovalDecisionPayload,
  ): Promise<ApprovalItem> {
    const endpoint =
      payload.decision === 'APPROVE'
        ? ApiEndpoints.generatedVersions.approve(workspaceId, generatedVersionId)
        : payload.decision === 'REJECT'
          ? ApiEndpoints.generatedVersions.reject(workspaceId, generatedVersionId)
          : ApiEndpoints.generatedVersions.requestChanges(workspaceId, generatedVersionId);

    const response = await firstValueFrom(
      this.api.post<ApprovalItem, ApprovalDecisionPayload>(
        endpoint,
        payload,
      ),
    );
    return unwrapApiResponse(response);
  }
}
