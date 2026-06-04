import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { WorkspaceCreditAccountView } from '../../models/credits.models';

@Component({ selector: 'app-free-credit-status-card', standalone: true, imports: [DatePipe], templateUrl: './free-credit-status-card.html', styleUrl: './free-credit-status-card.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class FreeCreditStatusCardComponent { readonly account = input<WorkspaceCreditAccountView | null>(null); }

