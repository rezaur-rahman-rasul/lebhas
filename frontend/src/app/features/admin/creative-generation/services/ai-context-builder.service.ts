import { Injectable } from '@angular/core';

import { Brand } from '@app/features/brands/brand.models';
import { ProductServiceRecord } from '@app/features/product-services/product-service.models';
import { ProjectCampaign } from '@app/features/projects/project.models';
import {
  AiCreativeGenerateRequest,
  AiCreativePlatform,
  AiCreativeQuality,
  AiCreativeSize,
  AiCreativeTone,
  AiCreativeType,
  AiGenerationMode,
  AiModelQuality,
  AiOutputFormat,
} from '../models/creative-generation.models';

export interface AiBrandContext {
  readonly brandId: string | null;
  readonly brandName: string | null;
  readonly businessType: string | null;
  readonly industry: string | null;
  readonly targetAudience: string | null;
  readonly creativeLanguage: string | null;
  readonly slogan: string | null;
  readonly preferredCta: string | null;
  readonly primaryColor: string | null;
  readonly secondaryColor: string | null;
  readonly logoAssetId: string | null;
  readonly website: string | null;
  readonly facebookUrl: string | null;
  readonly instagramUrl: string | null;
  readonly linkedinUrl: string | null;
  readonly tiktokUrl: string | null;
}

export interface AiProductContext {
  readonly productId: string | null;
  readonly productName: string | null;
  readonly category: string | null;
  readonly description: string | null;
  readonly targetAudience: string | null;
  readonly sellingPoints: string | null;
}

export interface AiCampaignContext {
  readonly campaignId: string | null;
  readonly campaignName: string | null;
  readonly description: string | null;
  readonly campaignObjective: string | null;
  readonly campaignType: string | null;
  readonly targetPlatform: string | null;
}

export interface AiCreativeScreenContext {
  readonly platform: string | null;
  readonly language: string | null;
  readonly creativeType: string | null;
  readonly tone: string | null;
  readonly quality: string | null;
  readonly campaignIdea: string | null;
  readonly includeCta: boolean | null;
  readonly includeLogo: boolean | null;
  readonly includeTypography: boolean | null;
  readonly headline: string | null;
  readonly subheadline: string | null;
  readonly offerText: string | null;
  readonly cta: string | null;
  readonly targetAudience: string | null;
  readonly backgroundPrompt: string | null;
  readonly transparentBackground: boolean | null;
  readonly versions: number | null;
  readonly productImage: boolean | null;
  readonly referenceImage: boolean | null;
  readonly maskImage: boolean | null;
}

export interface AiContextBuilderInput {
  readonly brand: Brand | null;
  readonly product: ProductServiceRecord | null;
  readonly campaign: ProjectCampaign | null;
  readonly screen: AiCreativeScreenContext;
  readonly logoAssetId: string | null;
}

export interface AiGenerationContext {
  readonly brand: AiBrandContext;
  readonly product: AiProductContext;
  readonly campaign: AiCampaignContext;
  readonly screen: AiCreativeScreenContext;
  readonly resolved: {
    readonly brandName: string | null;
    readonly productName: string | null;
    readonly campaignName: string | null;
    readonly language: string | null;
    readonly audience: string | null;
    readonly cta: string | null;
    readonly objective: string | null;
    readonly platform: string | null;
    readonly creativeType: string | null;
    readonly tone: string | null;
    readonly quality: string | null;
    readonly headline: string | null;
    readonly subheadline: string | null;
    readonly offerText: string | null;
    readonly productDescription: string | null;
    readonly brandColors: readonly string[];
  };
  readonly prompt: string;
}

export interface AiCreativePayloadInput {
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId: string | null;
  readonly campaignId: string | null;
  readonly promptTitlePreview: string | null;
  readonly platform: AiCreativePlatform;
  readonly language: string;
  readonly creativeType: AiCreativeType;
  readonly tone: AiCreativeTone;
  readonly modelQuality: AiModelQuality;
  readonly generationModeHint: AiGenerationMode;
  readonly versions: number;
  readonly size: AiCreativeSize;
  readonly quality: AiCreativeQuality;
  readonly outputFormat: AiOutputFormat;
  readonly background: 'opaque' | 'transparent';
  readonly includeCta: boolean;
  readonly includeLogo: boolean;
  readonly includeTypography: boolean;
  readonly noHumanModel: boolean;
  readonly existingAssetId?: string;
  readonly logoAssetId?: string;
  readonly referenceImage?: File;
  readonly maskImage?: File;
  readonly backgroundPrompt?: string;
}

@Injectable({ providedIn: 'root' })
export class AiContextBuilderService {
  build(input: AiContextBuilderInput): AiGenerationContext {
    const brand = this.resolveBrand(input.brand, input.logoAssetId);
    const product = this.resolveProduct(input.product);
    const campaign = this.resolveCampaign(input.campaign);
    const screen = this.resolveScreen(input.screen);
    const includeTypography = screen.includeTypography !== false;
    const includeCta = screen.includeCta === true && includeTypography;
    const resolved = {
      brandName: brand.brandName,
      productName: product.productName,
      campaignName: campaign.campaignName,
      language: firstValue(screen.language, brand.creativeLanguage),
      audience: firstValue(screen.targetAudience, product.targetAudience, brand.targetAudience),
      cta: includeCta ? firstValue(screen.cta, brand.preferredCta) : null,
      objective: campaign.campaignObjective,
      platform: firstValue(screen.platform, campaign.targetPlatform),
      creativeType: screen.creativeType,
      tone: screen.tone,
      quality: screen.quality,
      headline: includeTypography ? screen.headline : null,
      subheadline: includeTypography ? screen.subheadline : null,
      offerText: includeTypography ? screen.offerText : null,
      productDescription: joinValues([product.description, product.sellingPoints, product.productName ? `Product name: ${product.productName}` : null]),
      brandColors: [brand.primaryColor, brand.secondaryColor].filter(isPresent),
    } as const;

    return {
      brand,
      product,
      campaign,
      screen,
      resolved,
      prompt: this.buildPrompt(brand, product, campaign, screen, resolved),
    };
  }

  buildCreativePayload(context: AiGenerationContext, input: AiCreativePayloadInput): AiCreativeGenerateRequest {
    return {
      workspaceId: input.workspaceId,
      brandId: input.brandId,
      productServiceId: input.productServiceId || undefined,
      campaignId: input.campaignId || undefined,
      promptTitlePreview: input.promptTitlePreview || undefined,
      platform: input.platform,
      language: input.language,
      creativeType: input.creativeType,
      tone: input.tone,
      modelQuality: input.modelQuality,
      generationModeHint: input.generationModeHint,
      campaignIdea: context.prompt,
      headline: input.includeTypography ? context.resolved.headline ?? undefined : undefined,
      subheadline: input.includeTypography ? context.resolved.subheadline ?? undefined : undefined,
      offerText: input.includeTypography ? context.resolved.offerText ?? undefined : undefined,
      targetAudience: context.resolved.audience ?? undefined,
      productDescription: context.resolved.productDescription ?? undefined,
      campaignObjective: context.resolved.objective ?? undefined,
      cta: input.includeCta && input.includeTypography ? context.resolved.cta : null,
      includeCta: input.includeCta,
      includeLogo: input.includeLogo,
      includeTypography: input.includeTypography,
      versions: input.versions,
      size: input.size,
      quality: input.quality,
      outputFormat: input.outputFormat,
      background: input.background,
      noHumanModel: input.noHumanModel,
      existingAssetId: input.existingAssetId,
      logoAssetId: input.logoAssetId,
      referenceImage: input.referenceImage,
      maskImage: input.maskImage,
      backgroundPrompt: input.backgroundPrompt,
    };
  }

  private resolveBrand(brand: Brand | null, logoAssetId: string | null): AiBrandContext {
    return {
      brandId: clean(brand?.id),
      brandName: clean(brand?.name),
      businessType: clean(brand?.businessType),
      industry: clean(brand?.industry),
      targetAudience: clean(brand?.targetAudience),
      creativeLanguage: clean(brand?.languagePreference),
      slogan: null,
      preferredCta: clean(brand?.preferredCta),
      primaryColor: clean(brand?.primaryColor),
      secondaryColor: clean(brand?.secondaryColor),
      logoAssetId: clean(logoAssetId),
      website: clean(brand?.website),
      facebookUrl: clean(brand?.facebookUrl),
      instagramUrl: clean(brand?.instagramUrl),
      linkedinUrl: clean(brand?.linkedinUrl),
      tiktokUrl: clean(brand?.tiktokUrl),
    };
  }

  private resolveProduct(product: ProductServiceRecord | null): AiProductContext {
    return {
      productId: clean(product?.id),
      productName: clean(product?.name),
      category: clean(product?.category),
      description: clean(product?.description),
      targetAudience: clean(product?.targetAudience),
      sellingPoints: clean(product?.sellingPoints),
    };
  }

  private resolveCampaign(campaign: ProjectCampaign | null): AiCampaignContext {
    return {
      campaignId: clean(campaign?.id),
      campaignName: clean(campaign?.name),
      description: clean(campaign?.description),
      campaignObjective: clean(campaign?.campaignObjective),
      campaignType: clean(campaign?.campaignType),
      targetPlatform: clean(campaign?.targetPlatform),
    };
  }

  private resolveScreen(screen: AiCreativeScreenContext): AiCreativeScreenContext {
    return {
      platform: clean(screen.platform),
      language: clean(screen.language),
      creativeType: clean(screen.creativeType),
      tone: clean(screen.tone),
      quality: clean(screen.quality),
      campaignIdea: clean(screen.campaignIdea),
      includeCta: screen.includeCta ?? null,
      includeLogo: screen.includeLogo ?? null,
      includeTypography: screen.includeTypography ?? null,
      headline: clean(screen.headline),
      subheadline: clean(screen.subheadline),
      offerText: clean(screen.offerText),
      cta: clean(screen.cta),
      targetAudience: clean(screen.targetAudience),
      backgroundPrompt: clean(screen.backgroundPrompt),
      transparentBackground: screen.transparentBackground ?? null,
      versions: screen.versions ?? null,
      productImage: screen.productImage ?? null,
      referenceImage: screen.referenceImage ?? null,
      maskImage: screen.maskImage ?? null,
    };
  }

  private buildPrompt(
    brand: AiBrandContext,
    product: AiProductContext,
    campaign: AiCampaignContext,
    screen: AiCreativeScreenContext,
    resolved: AiGenerationContext['resolved'],
  ): string {
    const sections = [
      section('Brand', [
        field('Name', brand.brandName),
        field('Business Type', brand.businessType),
        field('Industry', brand.industry),
        field('Slogan', brand.slogan),
        field('Website', brand.website),
        field('Brand Colors', resolved.brandColors.join(', ')),
      ]),
      section('Product / Service', [
        field('Name', product.productName),
        field('Category', product.category),
        field('Description', product.description),
        field('Selling Points', product.sellingPoints),
      ]),
      section('Campaign', [
        field('Name', campaign.campaignName),
        field('Description', campaign.description),
        field('Objective', campaign.campaignObjective),
        field('Type', campaign.campaignType),
        field('Target Platform', campaign.targetPlatform),
      ]),
      section('Creative Direction', [
        field('Platform', resolved.platform),
        field('Language', resolved.language),
        field('Creative Type', resolved.creativeType),
        field('Tone', resolved.tone),
        field('Quality', resolved.quality),
        field('Audience', resolved.audience),
        field('CTA', resolved.cta),
        field('Headline', resolved.headline),
        field('Subheadline', resolved.subheadline),
        field('Offer Text', resolved.offerText),
        field('User Campaign Idea', screen.campaignIdea),
        field('Background Prompt', screen.backgroundPrompt),
      ]),
      'Generate a professional conversion-focused advertising creative.',
    ].filter(isPresent);

    return sections.join('\n\n');
  }
}

function section(title: string, rows: readonly (string | null)[]): string | null {
  const presentRows = rows.filter(isPresent);
  return presentRows.length > 0 ? `${title}:\n${presentRows.join('\n')}` : null;
}

function field(label: string, value: string | null): string | null {
  return value ? `${label}: ${value}` : null;
}

function clean(value: string | null | undefined): string | null {
  const trimmed = value?.replace(/\s+/g, ' ').trim();
  return trimmed ? trimmed : null;
}

function firstValue(...values: readonly (string | null | undefined)[]): string | null {
  return values.map(clean).find(isPresent) ?? null;
}

function joinValues(values: readonly (string | null | undefined)[]): string | null {
  const present = values.map(clean).filter(isPresent);
  return present.length > 0 ? present.join('. ') : null;
}

function isPresent(value: string | null | undefined): value is string {
  return Boolean(value);
}
