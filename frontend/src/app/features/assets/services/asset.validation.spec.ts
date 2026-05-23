import { validateUploadPayload } from './asset.validation';

function createFile(name: string, size: number, type = 'image/jpeg'): File {
  const buffer = new ArrayBuffer(size);
  return new File([buffer], name, { type });
}

describe('asset.validation', () => {
  it('requires project context', () => {
    const errors = validateUploadPayload(
      {
        projectId: '',
        file: createFile('product.jpg', 1024),
        assetCategory: 'PRODUCT_IMAGE',
        displayName: 'Studio flat lay',
        description: '',
        tags: [],
      },
      'workspace-1',
    );

    expect(errors['projectId']).toBeTruthy();
  });

  it('rejects unsupported file type', () => {
    const errors = validateUploadPayload(
      {
        projectId: 'project-1',
        file: createFile('notes.pdf', 1024, 'application/pdf'),
        assetCategory: 'PRODUCT_IMAGE',
        displayName: 'Invalid upload',
        description: '',
        tags: [],
      },
      'workspace-1',
    );

    expect(errors['file']).toBeTruthy();
  });

  it('rejects oversized image', () => {
    const errors = validateUploadPayload(
      {
        projectId: 'project-1',
        file: createFile('large.jpg', 11 * 1024 * 1024),
        assetCategory: 'PRODUCT_IMAGE',
        displayName: 'Large image',
        description: '',
        tags: [],
      },
      'workspace-1',
    );

    expect(errors['file']).toBeTruthy();
  });

  it('rejects oversized video', () => {
    const errors = validateUploadPayload(
      {
        projectId: 'project-1',
        file: createFile('large.mp4', 201 * 1024 * 1024, 'video/mp4'),
        assetCategory: 'PRODUCT_VIDEO',
        displayName: 'Large video',
        description: '',
        tags: [],
      },
      'workspace-1',
    );

    expect(errors['file']).toBeTruthy();
  });

  it('accepts valid upload payload', () => {
    const errors = validateUploadPayload(
      {
        projectId: 'project-1',
        file: createFile('product.jpg', 512 * 1024),
        assetCategory: 'PRODUCT_IMAGE',
        displayName: 'Product flat lay',
        description: '',
        tags: ['studio'],
      },
      'workspace-1',
    );

    expect(Object.keys(errors)).toEqual([]);
  });
});
