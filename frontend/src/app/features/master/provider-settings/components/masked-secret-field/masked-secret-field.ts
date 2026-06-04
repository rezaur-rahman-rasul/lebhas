import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { AiProviderCredentialView, maskProviderSecret } from '../../models/provider-credit-exchange.models';

@Component({
  selector: 'app-masked-secret-field',
  standalone: true,
  imports: [DatePipe, ButtonComponent, BadgeComponent],
  templateUrl: './masked-secret-field.html',
  styleUrl: './masked-secret-field.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MaskedSecretFieldComponent {
  readonly credential = input.required<AiProviderCredentialView>();
  readonly rotate = output<AiProviderCredentialView>();
  readonly deactivate = output<AiProviderCredentialView>();

  protected safeMasked(value: string): string {
    return maskProviderSecret(value);
  }
}
