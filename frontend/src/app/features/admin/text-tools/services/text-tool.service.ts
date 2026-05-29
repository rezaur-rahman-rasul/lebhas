import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import {
  TextToolHistoryItem,
  TextToolKind,
  TextToolRequest,
  TextToolResult,
} from '../models/text-tool.models';

interface TextToolResultDto {
  readonly id?: string | null;
  readonly tool?: TextToolKind | string | null;
  readonly title?: string | null;
  readonly generatedPost?: string | null;
  readonly post?: string | null;
  readonly primaryCaption?: string | null;
  readonly caption?: string | null;
  readonly primaryText?: string | null;
  readonly adCopy?: string | null;
  readonly headline?: string | null;
  readonly shortHeadline?: string | null;
  readonly description?: string | null;
  readonly cta?: string | null;
  readonly ctaText?: string | null;
  readonly platformNotes?: string | null;
  readonly alternativeCaptions?: readonly string[] | null;
  readonly recommendedHashtags?: readonly string[] | null;
  readonly localHashtags?: readonly string[] | null;
  readonly campaignHashtags?: readonly string[] | null;
  readonly hashtags?: readonly string[] | null;
  readonly creditCost?: number | null;
  readonly createdAt?: string | null;
}

interface TextToolHistoryDto {
  readonly id?: string | null;
  readonly tool?: TextToolKind | string | null;
  readonly title?: string | null;
  readonly preview?: string | null;
  readonly output?: string | null;
  readonly createdAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class TextToolService {
  private readonly api = inject(ApiService);

  generate(
    workspaceId: string,
    tool: TextToolKind,
    payload: TextToolRequest,
    context?: HttpContext,
  ) {
    return this.api
      .post<TextToolResultDto, TextToolRequest>(
        `/api/v1/workspaces/${workspaceId}/text-tools/${tool}`,
        payload,
        { context },
      )
      .pipe(map(({ data }) => mapTextToolResult(data, tool)));
  }

  history(workspaceId: string, tool: TextToolKind, context?: HttpContext) {
    return this.api
      .get<readonly TextToolHistoryDto[]>(
        `/api/v1/workspaces/${workspaceId}/text-tools/history`,
        {
          params: { tool, size: 6 },
          context,
        },
      )
      .pipe(map(({ data }) => data.map((item) => mapHistoryItem(item, tool))));
  }

  // TODO: Wire this to the backend template endpoint when text-tool template persistence is available.
  saveTemplate(workspaceId: string, resultId: string, context?: HttpContext) {
    return this.api.post<{ readonly id: string }, Record<string, never>>(
      `/api/v1/workspaces/${workspaceId}/text-tools/templates/${resultId}`,
      {},
      { context },
    );
  }
}

function mapTextToolResult(source: TextToolResultDto, fallbackTool: TextToolKind): TextToolResult {
  const tool = normalizeTool(source.tool) ?? fallbackTool;
  const sections = sectionsForTool(source, tool);

  return {
    id: source.id ?? null,
    tool,
    title: source.title ?? titleForTool(tool),
    sections,
    creditCost: source.creditCost ?? null,
    createdAt: source.createdAt ?? null,
  };
}

function mapHistoryItem(source: TextToolHistoryDto, fallbackTool: TextToolKind): TextToolHistoryItem {
  const tool = normalizeTool(source.tool) ?? fallbackTool;

  return {
    id: source.id ?? `${tool}-${source.createdAt ?? 'recent'}`,
    tool,
    title: source.title ?? titleForTool(tool),
    preview: source.preview ?? source.output ?? 'Generated text output',
    createdAt: source.createdAt ?? null,
  };
}

function sectionsForTool(source: TextToolResultDto, tool: TextToolKind) {
  switch (tool) {
    case 'post':
      return [
        section('Generated post', source.generatedPost ?? source.post),
        section('Short headline', source.shortHeadline ?? source.headline),
        section('CTA', source.ctaText ?? source.cta),
      ].filter((item) => item.body.length > 0);
    case 'caption':
      return [
        section('Primary caption', source.primaryCaption ?? source.caption),
        section('Alternative captions', joinLines(source.alternativeCaptions)),
      ].filter((item) => item.body.length > 0);
    case 'ads-copy':
      return [
        section('Primary text', source.primaryText ?? source.adCopy),
        section('Headline', source.headline),
        section('Description', source.description),
        section('CTA', source.ctaText ?? source.cta),
        section('Platform notes', source.platformNotes),
      ].filter((item) => item.body.length > 0);
    case 'hashtags':
      return [
        section('Recommended hashtags', joinLines(source.recommendedHashtags ?? source.hashtags)),
        section('Local / Bangladesh hashtags', joinLines(source.localHashtags)),
        section('Campaign hashtags', joinLines(source.campaignHashtags)),
      ].filter((item) => item.body.length > 0);
  }
}

function section(title: string, value: string | null | undefined) {
  return {
    title,
    body: value?.trim() ?? '',
    copyLabel: title === 'CTA' ? 'Copy CTA' : `Copy ${title.toLowerCase()}`,
  };
}

function joinLines(values: readonly string[] | null | undefined): string {
  return values?.filter(Boolean).join('\n') ?? '';
}

function normalizeTool(value: TextToolKind | string | null | undefined): TextToolKind | null {
  if (value === 'post' || value === 'caption' || value === 'ads-copy' || value === 'hashtags') {
    return value;
  }

  return null;
}

function titleForTool(tool: TextToolKind): string {
  switch (tool) {
    case 'post':
      return 'Generated post';
    case 'caption':
      return 'Generated captions';
    case 'ads-copy':
      return 'Generated ad copy';
    case 'hashtags':
      return 'Generated hashtags';
  }
}

