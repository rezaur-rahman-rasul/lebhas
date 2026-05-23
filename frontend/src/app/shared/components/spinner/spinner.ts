import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-spinner',
  standalone: true,
  templateUrl: './spinner.html',
  styleUrl: './spinner.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpinnerComponent {
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  protected readonly classes = computed(() => {
    const sizes = {
      sm: 'h-4 w-4 border-2',
      md: 'h-5 w-5 border-2',
      lg: 'h-7 w-7 border-[3px]',
    };

    return `${sizes[this.size()]} inline-block animate-spin rounded-full border-current border-t-transparent`;
  });
}
