import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import {NgOptimizedImage} from '@angular/common';

type BrandLogoSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-brand-logo',
  standalone: true,
  templateUrl: './brand-logo.html',
  styleUrl: './brand-logo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgOptimizedImage
  ]
})
export class BrandLogoComponent {
  readonly size = input<BrandLogoSize>('md');
  readonly elevated = input(true);
}
