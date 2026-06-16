import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { IconComponent } from '@app/shared/components/icon/icon';
import { AiProviderView, SmsProviderActionResult } from '../../models/provider-credit-exchange.models';
import { ProviderStatusBadgeComponent } from '../provider-status-badge/provider-status-badge';

@Component({
  selector: 'app-provider-card',
  standalone: true,
  imports: [ButtonComponent, IconComponent, ProviderStatusBadgeComponent],
  templateUrl: './provider-card.html',
  styleUrl: './provider-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderCardComponent {
  readonly provider = input.required<AiProviderView>();
  readonly selected = input(false);
  readonly balanceResult = input<SmsProviderActionResult | null>(null);
  readonly selectProvider = output<AiProviderView>();
  readonly configureCredentials = output<AiProviderView>();
  readonly editProvider = output<AiProviderView>();
  readonly toggleProvider = output<AiProviderView>();
  readonly deleteProvider = output<AiProviderView>();
  readonly testProvider = output<AiProviderView>();
  readonly testSms = output<AiProviderView>();
  readonly checkSmsBalance = output<AiProviderView>();
  readonly viewModelsJson = output<AiProviderView>();
  readonly viewCreditPool = output<AiProviderView>();
  readonly exchangePolicy = output<AiProviderView>();
  readonly syncCosts = output<AiProviderView>();

  protected canShowModelsJson(provider: AiProviderView): boolean {
    const providerType = (provider.providerType || provider.category || '').toUpperCase();
    const providerCode = provider.providerCode.toUpperCase();
    return Boolean(provider.modelsEndpoint?.trim())
      && providerType !== 'PAYMENT'
      && !['STRIPE', 'SSLCOMMERZ', 'BKASH', 'NAGAD'].includes(providerCode);
  }

  protected isSmsProvider(provider: AiProviderView): boolean {
    return String(provider.providerCategory || provider.providerType || provider.category || '').toUpperCase() === 'SMS';
  }

  protected isOpenAiProvider(provider: AiProviderView): boolean {
    return provider.providerCode.toUpperCase() === 'OPENAI';
  }

  protected creditBalanceLabel(provider: AiProviderView): string {
    const balance = this.normalizeOptionalNumber(provider.availableCreditBalance);
    if (balance === null) {
      return 'Not set';
    }
    return new Intl.NumberFormat('en-US', {
      currency: 'USD',
      maximumFractionDigits: 2,
      minimumFractionDigits: 2,
      style: 'currency',
    }).format(balance);
  }

  protected smsBalanceLabel(result: SmsProviderActionResult | null): string {
    const response = result?.response;
    const balance = this.normalizeOptionalNumber(
      response?.['balance'] ??
      response?.['availableCreditBalance'] ??
      response?.['available_balance'] ??
      response?.['credit_balance'] ??
      response?.['total_available'],
    );
    if (balance === null) {
      return 'Not checked';
    }
    return new Intl.NumberFormat('en-US', {
      maximumFractionDigits: 2,
      minimumFractionDigits: 2,
    }).format(balance);
  }

  protected moneyLabel(value: unknown): string {
    const amount = this.normalizeOptionalNumber(value);
    if (amount === null) {
      return 'Not set';
    }
    return new Intl.NumberFormat('en-US', {
      currency: 'USD',
      maximumFractionDigits: 2,
      minimumFractionDigits: 2,
      style: 'currency',
    }).format(amount);
  }

  protected creditsLabel(value: unknown): string {
    const amount = this.normalizeOptionalNumber(value);
    if (amount === null) {
      return 'Not set';
    }
    return new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 }).format(Math.floor(amount));
  }

  protected dateTimeLabel(value: string | null | undefined): string {
    if (!value) {
      return 'Never';
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'Never' : date.toLocaleString();
  }

  private normalizeOptionalNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const normalized = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(normalized) ? normalized : null;
  }
}
