export type BrandStatus = 'ACTIVE' | 'ARCHIVED';

export interface Brand {
  readonly id: string;
  readonly workspaceId: string;
  readonly ownerUserId: string;
  readonly name: string;
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
  readonly status: BrandStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateBrandPayload {
  readonly name: string;
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
}

export interface UpdateBrandPayload extends CreateBrandPayload {
  readonly status: BrandStatus;
}
