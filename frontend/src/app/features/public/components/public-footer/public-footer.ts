import { ChangeDetectionStrategy, Component } from '@angular/core';

interface FooterLink {
  readonly label: string;
  readonly href: string;
}

@Component({
  selector: 'app-public-footer',
  standalone: true,
  templateUrl: './public-footer.html',
  styleUrl: './public-footer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicFooterComponent {
  protected readonly links: readonly FooterLink[] = [
    { label: 'Home', href: '#home' },
    { label: 'Features', href: '#features' },
    { label: 'Pricing', href: '#pricing' },
    { label: 'Platforms', href: '#platforms' },
  ];
}
