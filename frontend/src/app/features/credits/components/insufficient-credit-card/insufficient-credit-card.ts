import { ChangeDetectionStrategy, Component, output } from '@angular/core';

@Component({ selector: 'app-insufficient-credit-card', standalone: true, templateUrl: './insufficient-credit-card.html', styleUrl: './insufficient-credit-card.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class InsufficientCreditCardComponent { readonly buyCredits = output<void>(); readonly reduceVersions = output<void>(); }
