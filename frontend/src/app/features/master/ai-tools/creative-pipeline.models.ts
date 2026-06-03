export type CreativeLayerType =
  | 'INPUT_UNDERSTANDING'
  | 'PRODUCT_ANALYSIS'
  | 'SEGMENTATION'
  | 'BRAND_RULES'
  | 'PROMPT_ENGINEERING'
  | 'MODEL_GENERATION'
  | 'PRODUCT_PRESERVATION'
  | 'BACKGROUND_GENERATION'
  | 'TEXT_RENDERING'
  | 'LOGO_COMPOSITION'
  | 'QUALITY_CONTROL'
  | 'EXPORT'
  | 'BILLING_FINALIZATION'
  | string;

export type CreativePipelineStatus = 'DRAFT' | 'ACTIVE' | 'DISABLED' | 'ARCHIVED' | string;
export type LayerRoutingStrategy = 'COST_OPTIMIZED' | 'QUALITY_OPTIMIZED' | 'FASTEST' | 'BALANCED' | string;

export interface LayerToolMappingView {
  readonly id: string;
  readonly pipelineLayerId: string;
  readonly providerId: string;
  readonly modelId: string | null;
  readonly capabilityId: string | null;
  readonly mappingCode: string;
  readonly priorityOrder: number;
  readonly routingWeight: number;
  readonly enabled: boolean;
  readonly fallbackEligible: boolean;
  readonly routingMetadata: Readonly<Record<string, unknown>>;
}

export interface LayerRoutingPolicyView {
  readonly id: string;
  readonly pipelineLayerId: string;
  readonly policyCode: string;
  readonly routingStrategy: LayerRoutingStrategy;
  readonly priorityOrder: number;
  readonly enabled: boolean;
  readonly conditions: Readonly<Record<string, unknown>>;
  readonly rules: Readonly<Record<string, unknown>>;
}

export interface LayerCostPolicyView {
  readonly id: string;
  readonly pipelineLayerId: string;
  readonly policyCode: string;
  readonly enabled: boolean;
  readonly priorityOrder: number;
  readonly currency: string;
  readonly maxCostPerRun: number | null;
  readonly costRules: Readonly<Record<string, unknown>>;
  readonly budgetMetadata: Readonly<Record<string, unknown>>;
}

export interface LayerQualityPolicyView {
  readonly id: string;
  readonly pipelineLayerId: string;
  readonly policyCode: string;
  readonly enabled: boolean;
  readonly priorityOrder: number;
  readonly minQualityScore: number | null;
  readonly qualityRules: Readonly<Record<string, unknown>>;
  readonly evaluationMetadata: Readonly<Record<string, unknown>>;
}

export interface CreativePipelineLayerView {
  readonly id: string;
  readonly pipelineId: string;
  readonly layerType: CreativeLayerType;
  readonly layerCode: string;
  readonly layerName: string;
  readonly sortOrder: number;
  readonly enabled: boolean;
  readonly required: boolean;
  readonly retryable: boolean;
  readonly configuration: Readonly<Record<string, unknown>>;
  readonly toolMappings: readonly LayerToolMappingView[];
  readonly routingPolicies: readonly LayerRoutingPolicyView[];
  readonly costPolicies: readonly LayerCostPolicyView[];
  readonly qualityPolicies: readonly LayerQualityPolicyView[];
}

export interface CreativePipelineView {
  readonly id: string;
  readonly pipelineCode: string;
  readonly pipelineName: string;
  readonly description: string | null;
  readonly status: CreativePipelineStatus;
  readonly active: boolean;
  readonly version: number;
  readonly metadata: Readonly<Record<string, unknown>>;
  readonly layers: readonly CreativePipelineLayerView[];
}

export interface ConfigureLayerRoutingPolicyPayload {
  readonly policyCode: string;
  readonly routingStrategy: LayerRoutingStrategy;
  readonly priorityOrder: number;
  readonly enabled: boolean;
  readonly conditions: Readonly<Record<string, unknown>>;
  readonly rules: Readonly<Record<string, unknown>>;
}

export interface CreativeToolView {
  readonly id: string;
  readonly toolCode: string;
  readonly toolName: string;
  readonly toolCategory: string;
  readonly enabled: boolean;
  readonly description?: string | null;
}

export interface ProviderRoutingPolicyView {
  readonly id: string;
  readonly policyCode: string;
  readonly toolId: string;
  readonly qualityMode: string;
  readonly providerId: string;
  readonly modelId?: string | null;
  readonly fallbackProviderId?: string | null;
  readonly fallbackModelId?: string | null;
  readonly priorityOrder: number;
  readonly enabled: boolean;
  readonly circuitFailureThreshold: number;
}
