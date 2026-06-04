import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { GenerationCreditPreviewView } from '../../models/credits.models';

@Component({ selector: 'app-credit-usage-preview-card', standalone: true, imports: [DecimalPipe], templateUrl: './credit-usage-preview-card.html', styleUrl: './credit-usage-preview-card.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class CreditUsagePreviewCardComponent { readonly preview = input<GenerationCreditPreviewView | null>(null); }
