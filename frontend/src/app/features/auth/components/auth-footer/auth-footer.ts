import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-auth-footer',
  standalone: true,
  templateUrl: './auth-footer.html',
  styleUrl: './auth-footer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthFooterComponent {
  protected readonly footerItems = [
    'Lebhas Creative Maker',
    'Create Ads Beyond Imagination',
    'Dhaka, Bangladesh',
  ];
}
