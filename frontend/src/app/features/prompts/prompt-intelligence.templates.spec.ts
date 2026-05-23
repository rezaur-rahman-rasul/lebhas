import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const promptsRoot = join(process.cwd(), 'src/app/features/prompts');

function collectFiles(directory: string, extension: string, files: string[] = []): string[] {
  for (const entry of readdirSync(directory)) {
    const fullPath = join(directory, entry);
    const stats = statSync(fullPath);

    if (stats.isDirectory()) {
      collectFiles(fullPath, extension, files);
      continue;
    }

    if (fullPath.endsWith(extension)) {
      files.push(fullPath);
    }
  }

  return files;
}

describe('Prompt Intelligence templates', () => {
  it('does not use legacy structural directives in prompt templates', () => {
    const htmlFiles = collectFiles(promptsRoot, '.html');
    const offenders = htmlFiles.filter((file) => {
      const contents = readFileSync(file, 'utf8');
      return /\*ngIf\b/.test(contents) || /\*ngFor\b/.test(contents);
    });

    expect(offenders).toEqual([]);
  });

  it('uses theme tokens and responsive layout classes', () => {
    const htmlFiles = collectFiles(promptsRoot, '.html');
    const scssFiles = collectFiles(promptsRoot, '.scss');
    const globalStyles = readFileSync(join(process.cwd(), 'src/styles.scss'), 'utf8');
    const combined = [...htmlFiles, ...scssFiles]
      .map((file) => readFileSync(file, 'utf8'))
      .join('\n');

    expect(globalStyles).toContain('html.light');
    expect(globalStyles).toContain('html.dark');
    expect(combined).toMatch(/text-ink/);
    expect(combined).toMatch(/border-border/);
    expect(combined).toMatch(/(?:sm:|md:|lg:|xl:|2xl:)/);
    expect(combined).toMatch(/(?:min-w-0|flex-wrap|grid)/);
  });
});
