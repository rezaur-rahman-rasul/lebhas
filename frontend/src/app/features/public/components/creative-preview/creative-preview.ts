import { ChangeDetectionStrategy, Component } from '@angular/core';

interface CreativePreviewCard {
  readonly title: string;
  readonly description: string;
  readonly imageUrl: string;
  readonly tone: string;
}

@Component({
  selector: 'app-creative-preview',
  standalone: true,
  templateUrl: './creative-preview.html',
  styleUrl: './creative-preview.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativePreviewComponent {
  protected readonly previews: readonly CreativePreviewCard[] = [
    {
      title: 'Source product pack',
      description: 'The workspace starts from flat-lays, packaging shots, and brand-approved product visuals.',
      imageUrl: '/assets/product-flatlay.png',
      tone: 'Raw',
    },
    {
      title: 'AI campaign poster',
      description: 'A product-first ad treatment prepared for social placements without relying on human model imagery.',
      imageUrl: '/assets/campaign-poster.png',
      tone: 'Enhanced',
    },
    {
      title: 'Workspace shell',
      description: 'Role-aware cards, recent activity, and project placeholders live inside the protected dashboard foundation.',
      imageUrl: '/assets/workflow-dashboard.png',
      tone: 'Export',
    },
  ];
}
