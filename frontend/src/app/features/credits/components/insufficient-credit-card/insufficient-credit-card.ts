import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';

@Component({ selector: 'app-insufficient-credit-card', standalone: true, imports: [ButtonComponent], templateUrl: './insufficient-credit-card.html', styleUrl: './insufficient-credit-card.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class InsufficientCreditCardComponent { readonly buyCredits = output<void>(); readonly reduceVersions = output<void>(); }
