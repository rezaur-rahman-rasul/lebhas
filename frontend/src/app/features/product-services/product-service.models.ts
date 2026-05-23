export type ProductServiceStatus = 'ACTIVE' | 'ARCHIVED';

export interface ProductServiceRecord {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly name: string;
  readonly description: string | null;
  readonly category: string | null;
  readonly targetAudience: string | null;
  readonly sellingPoints: string | null;
  readonly status: ProductServiceStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateProductServicePayload {
  readonly name: string;
  readonly description: string | null;
  readonly category: string | null;
  readonly targetAudience: string | null;
  readonly sellingPoints: string | null;
}

export interface UpdateProductServicePayload extends CreateProductServicePayload {
  readonly status: ProductServiceStatus;
}
