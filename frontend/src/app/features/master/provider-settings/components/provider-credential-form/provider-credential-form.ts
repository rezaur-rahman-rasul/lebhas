import { ChangeDetectionStrategy, Component, effect, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { IconComponent } from '@app/shared/components/icon/icon';
import { AiProviderCredentialView, CreateProviderCredentialRequest, ProviderEnvironment } from '../../models/provider-credit-exchange.models';

@Component({
  selector: 'app-provider-credential-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, IconComponent],
  templateUrl: './provider-credential-form.html',
  styleUrl: './provider-credential-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderCredentialFormComponent {
  readonly credential = input<AiProviderCredentialView | null>(null);
  readonly saving = input(false);
  readonly saved = output<CreateProviderCredentialRequest>();
  readonly closed = output<void>();

  protected readonly revealTypedKey = signal(false);
  protected readonly form = new FormGroup({
    credentialName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    environment: new FormControl<ProviderEnvironment>('SANDBOX', { nonNullable: true, validators: [Validators.required] }),
    apiKey: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    active: new FormControl(true, { nonNullable: true }),
  });

  constructor() {
    effect(() => {
      const credential = this.credential();
      this.form.reset({
        credentialName: credential?.credentialName ?? '',
        environment: credential?.environment ?? 'SANDBOX',
        apiKey: '',
        active: credential?.active ?? true,
      });
      this.revealTypedKey.set(false);
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saved.emit(value);
    this.clearSecret();
  }

  close(): void {
    this.clearSecret();
    this.closed.emit();
  }

  clearSecret(): void {
    this.form.controls.apiKey.setValue('');
    this.revealTypedKey.set(false);
  }
}
