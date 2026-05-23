import { PROMPT_MAX_LENGTH, PROMPT_MIN_LENGTH } from '../models/prompt.models';
import { validateSourcePrompt, validateTemplatePayload } from './prompt.validation';

describe('prompt.validation', () => {
  it('requires a minimum prompt length', () => {
    const errors = validateSourcePrompt('abc');

    expect(errors['sourcePrompt']).toContain(String(PROMPT_MIN_LENGTH));
  });

  it('rejects prompts above the maximum length', () => {
    const errors = validateSourcePrompt('x'.repeat(PROMPT_MAX_LENGTH + 1));

    expect(errors['sourcePrompt']).toContain(String(PROMPT_MAX_LENGTH));
  });

  it('accepts valid source prompts', () => {
    const errors = validateSourcePrompt('A valid campaign prompt for testing.');

    expect(Object.keys(errors)).toEqual([]);
  });

  it('requires template form fields', () => {
    const errors = validateTemplatePayload({
      name: '',
      category: null,
      description: null,
      platform: 'INSTAGRAM',
      campaignObjective: 'AWARENESS',
      businessType: null,
      language: 'ENGLISH',
      promptBody: '',
      isDefault: false,
      status: 'ACTIVE',
    });

    expect(errors['name']).toBeTruthy();
    expect(errors['promptBody']).toBeTruthy();
  });
});
