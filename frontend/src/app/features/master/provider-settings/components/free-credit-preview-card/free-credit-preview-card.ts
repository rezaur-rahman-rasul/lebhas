import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-free-credit-preview-card',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './free-credit-preview-card.html',
  styleUrl: './free-credit-preview-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FreeCreditPreviewCardComponent {
  readonly preview = input.required<{ readonly available: number; readonly percentage: number; readonly estimated: number; readonly maxCap: number; readonly fallback: number; readonly result: number }>();
}
