import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { AiQualityScore, QualityScoreLabel } from '../../models/ai-monitoring.models';

@Component({
  selector: 'app-quality-score-card',
  standalone: true,
  imports: [CardComponent, BadgeComponent],
  templateUrl: './quality-score-card.html',
  styleUrl: './quality-score-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QualityScoreCardComponent {
  readonly score = input.required<AiQualityScore>();
  readonly label = input<QualityScoreLabel | string | null>(null);

  protected readonly labelText = computed(() =>
    this.label()
      ? String(this.label())
          .toLowerCase()
          .replace(/_/g, ' ')
          .replace(/\b\w/g, (letter) => letter.toUpperCase())
      : 'Quality score',
  );

  protected readonly tone = computed(() => {
    switch (this.label()) {
      case QualityScoreLabel.Excellent:
      case QualityScoreLabel.Good:
        return 'brand';
      case QualityScoreLabel.NeedsImprovement:
        return 'blue';
      case QualityScoreLabel.Failed:
        return 'red';
      default:
        return 'neutral';
    }
  });
}
