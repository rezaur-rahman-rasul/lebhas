export interface BrandProfile {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandName: string | null;
  readonly businessType: string | null;
  readonly industry: string | null;
  readonly targetAudience: string | null;
  readonly brandVoice: string | null;
  readonly preferredCTA: string | null;
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

export interface UpdateBrandProfilePayload {
  readonly brandName: string;
  readonly businessType: string | null;
  readonly industry: string | null;
  readonly targetAudience: string | null;
  readonly brandVoice: string | null;
  readonly preferredCTA: string | null;
  readonly primaryColor: string | null;
  readonly secondaryColor: string | null;
  readonly website: string | null;
  readonly facebookUrl: string | null;
  readonly instagramUrl: string | null;
  readonly linkedinUrl: string | null;
  readonly tiktokUrl: string | null;
  readonly description: string | null;
}
