import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { BrandProfile, UpdateBrandProfilePayload } from '../models/brand-profile.models';

interface BrandProfileResponseDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandName: string | null;
  readonly businessType: string | null;
  readonly industry: string | null;
  readonly targetAudience: string | null;
  readonly brandVoice: string | null;
  readonly preferredCta: string | null;
  readonly primaryColor: string | null;
  readonly secondaryColor: string | null;
  readonly website: string | null;
  readonly facebookUrl: string | null;
  readonly instagramUrl: string | null;
  readonly linkedinUrl: string | null;
  readonly tiktokUrl: string | null;
  readonly description: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface UpdateBrandProfileRequestDto {
  readonly brandName: string;
  readonly businessType: string | null;
  readonly industry: string | null;
  readonly targetAudience: string | null;
  readonly brandVoice: string | null;
  readonly preferredCta: string | null;
  readonly primaryColor: string | null;
  readonly secondaryColor: string | null;
  readonly website: string | null;
  readonly facebookUrl: string | null;
  readonly instagramUrl: string | null;
  readonly linkedinUrl: string | null;
  readonly tiktokUrl: string | null;
  readonly description: string | null;
}

@Injectable({ providedIn: 'root' })
export class BrandProfileService {
  private readonly api = inject(ApiService);

  getBrandProfile(workspaceId: string, context?: HttpContext) {
    return this.api
      .get<BrandProfileResponseDto>(`/api/v1/workspaces/${workspaceId}/brand-profile`, { context })
      .pipe(map(({ data }) => mapBrandProfile(data)));
  }

  updateBrandProfile(workspaceId: string, payload: UpdateBrandProfilePayload) {
    return this.api
      .put<BrandProfileResponseDto, UpdateBrandProfileRequestDto>(
        `/api/v1/workspaces/${workspaceId}/brand-profile`,
        {
          brandName: payload.brandName,
          businessType: payload.businessType,
          industry: payload.industry,
          targetAudience: payload.targetAudience,
          brandVoice: payload.brandVoice,
          preferredCta: payload.preferredCTA,
          primaryColor: payload.primaryColor,
          secondaryColor: payload.secondaryColor,
          website: payload.website,
          facebookUrl: payload.facebookUrl,
          instagramUrl: payload.instagramUrl,
          linkedinUrl: payload.linkedinUrl,
          tiktokUrl: payload.tiktokUrl,
          description: payload.description,
        },
      )
      .pipe(map(({ data }) => mapBrandProfile(data)));
  }
}

function mapBrandProfile(source: BrandProfileResponseDto): BrandProfile {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    brandName: source.brandName,
    businessType: source.businessType,
    industry: source.industry,
    targetAudience: source.targetAudience,
    brandVoice: source.brandVoice,
    preferredCTA: source.preferredCta,
    primaryColor: source.primaryColor,
    secondaryColor: source.secondaryColor,
    website: source.website,
    facebookUrl: source.facebookUrl,
    instagramUrl: source.instagramUrl,
    linkedinUrl: source.linkedinUrl,
    tiktokUrl: source.tiktokUrl,
    description: source.description,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}
