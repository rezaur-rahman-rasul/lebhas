import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { describe, expect, it } from 'vitest';

import { DASHBOARD_NAVIGATION } from '../../../shared/layouts/dashboard-layout/dashboard-navigation';

const appRoot = join(process.cwd(), 'src/app');
const providerRoot = join(appRoot, 'features/master/provider-settings');
const creditsRoot = join(appRoot, 'features/credits');

function collect(directory: string, files: string[] = []): string[] {
  for (const entry of readdirSync(directory)) {
    const full = join(directory, entry);
    if (statSync(full).isDirectory()) {
      collect(full, files);
    } else {
      files.push(full);
    }
  }
  return files;
}

function read(path: string): string {
  return readFileSync(path, 'utf8');
}

function rel(path: string): string {
  return relative(appRoot, path).replaceAll('\\', '/');
}

describe('provider credit exchange frontend', () => {
  const files = [...collect(providerRoot), ...collect(creditsRoot)];
  const templates = files.filter((file) => file.endsWith('.html')).map(read).join('\n');
  const source = files.filter((file) => file.endsWith('.ts') || file.endsWith('.html')).map(read).join('\n');

  it('routes Master provider credit pages without exposing them to Admin navigation', () => {
    expect(DASHBOARD_NAVIGATION.some((item) => item.label === 'Provider Management' && item.roles?.includes('MASTER'))).toBe(true);
    expect(DASHBOARD_NAVIGATION.some((item) => item.label === 'Provider Credit Pools' && item.roles?.includes('MASTER'))).toBe(true);
    expect(DASHBOARD_NAVIGATION.some((item) => item.label === 'Exchange Policies' && item.roles?.includes('MASTER'))).toBe(true);
    expect(DASHBOARD_NAVIGATION.some((item) => item.label === 'Credit Overview' && item.roles?.includes('MASTER'))).toBe(true);
    expect(DASHBOARD_NAVIGATION.filter((item) => item.roles?.includes('ADMIN')).map((item) => item.label)).not.toContain('Provider Management');
    expect(DASHBOARD_NAVIGATION.filter((item) => item.roles?.includes('ADMIN')).map((item) => item.label)).not.toContain('Provider Credit Pools');
  });

  it('keeps provider secrets masked and out of stores', () => {
    expect(read(join(providerRoot, 'components/provider-credential-form/provider-credential-form.html'))).toContain("type]=" + '"revealTypedKey() ? \'text\' : \'password\'"');
    expect(read(join(providerRoot, 'components/masked-secret-field/masked-secret-field.ts'))).toContain('maskProviderSecret');
    expect(read(join(providerRoot, 'state/providerSettings.store.ts'))).not.toMatch(/apiKeySignal|secretSignal|rawApiKey|console\.log|localStorage|sessionStorage/);
    expect(source).not.toMatch(/sk-proj-[A-Za-z0-9_-]{12,}/);
  });

  it('uses Angular 21 block templates and required file naming', () => {
    expect(templates).not.toMatch(/\*ngIf\b|\*ngFor\b/);
    expect(files.map(rel).filter((file) => /\.component\.|\.css$/.test(file))).toEqual([]);
  });

  it('does not hardcode free signup percentage business logic', () => {
    expect(read(join(providerRoot, 'state/exchangePolicy.store.ts'))).not.toContain('* 0.02');
    expect(read(join(providerRoot, 'state/exchangePolicy.store.ts'))).toContain('freeSignupCreditPercentage');
    expect(read(join(providerRoot, 'components/free-credit-preview-card/free-credit-preview-card.html'))).toContain('backend policy percentage');
  });

  it('adds Admin internal credit UX and generation credit preview', () => {
    expect(read(join(creditsRoot, 'components/credit-balance-card/credit-balance-card.html'))).toContain('Credit Balance');
    expect(read(join(creditsRoot, 'components/insufficient-credit-card/insufficient-credit-card.html'))).toContain('Buy Credits');
    expect(read(join(appRoot, 'features/admin/creative-generation/pages/creative-generator/creative-generator.html'))).toContain('app-credit-usage-preview-card');
    expect(read(join(creditsRoot, 'components/credit-ledger-table/credit-ledger-table.ts'))).toContain('creditTransactionLabel');
  });
});

