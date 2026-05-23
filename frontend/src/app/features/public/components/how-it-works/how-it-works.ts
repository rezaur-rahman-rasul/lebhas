import { ChangeDetectionStrategy, Component } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

interface WorkflowStep {
  readonly title: string;
  readonly description: string;
  readonly icon: string;
}

@Component({
  selector: 'app-how-it-works',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './how-it-works.html',
  styleUrl: './how-it-works.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HowItWorksComponent {
  protected readonly steps: readonly WorkflowStep[] = [
    {
      title: 'Upload raw image',
      description: 'Start from the product photo or asset your business already has.',
      icon: 'cloud-upload',
    },
    {
      title: 'Select brand or project',
      description: 'Keep creative output attached to the correct workspace, brand, and campaign.',
      icon: 'building-2',
    },
    {
      title: 'Write or enhance prompt',
      description: 'Turn a rough idea into structured creative direction using prompt intelligence.',
      icon: 'pencil-line',
    },
    {
      title: 'Generate ad creative',
      description: 'Produce campaign-ready visuals aligned to the selected channel and format.',
      icon: 'sparkles',
    },
    {
      title: 'Download and market',
      description: 'Move the approved export set into your paid campaigns or organic promotions.',
      icon: 'download',
    },
  ];
}
