import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';

@Component({ selector: 'app-buy-credits-card', standalone: true, imports: [ButtonComponent], templateUrl: './buy-credits-card.html', styleUrl: './buy-credits-card.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class BuyCreditsCardComponent { readonly paymentAvailable = input(true); readonly buyCredits = output<void>(); }
