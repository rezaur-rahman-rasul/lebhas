import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { WorkspaceCreditAccountView } from '../../models/credits.models';

@Component({ selector: 'app-credit-balance-card', standalone: true, imports: [DecimalPipe], templateUrl: './credit-balance-card.html', styleUrl: './credit-balance-card.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class CreditBalanceCardComponent { readonly account = input<WorkspaceCreditAccountView | null>(null); }
