import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

describe('Home AI Creative Workflow light mode', () => {
  it('keeps the existing workflow section and modern Angular control flow', () => {
    const template = read('src/app/features/public/home/home.html');
    const source = read('src/app/features/public/home/home.ts');

    expect(source).toContain("title: 'AI Creative Workflow'");
    expect(template).toContain('{{ copy().workflow.title }}');
    expect(template).toContain('workflow-card');
    expect(template).toContain('workflow-steps');
    expect(template).toContain('@for (step of workflowSteps(); track step.step; let last = $last)');
    expect(template).not.toMatch(/\*ngIf\b|\*ngFor\b/);
  });

  it('adds readable light-mode workflow card, icon circle, and connector styling', () => {
    const styles = read('src/app/features/public/home/home.scss');

    expect(styles).toContain('.home-shell.light-mode .workflow-card');
    expect(styles).toContain('border-color: rgb(var(--color-border))');
    expect(styles).toContain('0 24px 64px rgba(15, 23, 42, 0.11)');
    expect(styles).toContain('.home-shell.light-mode .step-icon');
    expect(styles).toContain('background:');
    expect(styles).toContain('rgba(220, 252, 231, 0.82)');
    expect(styles).toContain('color: #047857');
    expect(styles).toContain('.home-shell.light-mode .step-line');
    expect(styles).toContain('border-top-color: rgba(71, 85, 105, 0.46)');
  });

  it('preserves dark-mode workflow base styles while improving light mode only', () => {
    const styles = read('src/app/features/public/home/home.scss');

    expect(styles).toContain('.workflow-card');
    expect(styles).toContain('.step-icon');
    expect(styles).toContain('.step-line');
    expect(styles).toContain('background: rgba(15, 23, 42, 0.78)');
    expect(styles).toContain('border-top: 1px dashed rgba(148, 163, 184, 0.35)');
  });
});
