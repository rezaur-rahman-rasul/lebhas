import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { InitialsPipe } from '../../pipes/initials.pipe';

@Component({
  selector: 'app-avatar',
  standalone: true,
  imports: [InitialsPipe],
  templateUrl: './avatar.html',
  styleUrl: './avatar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvatarComponent {
  readonly name = input('User');
  readonly avatarUrl = input<string | null>(null);
  readonly size = input<'sm' | 'md' | 'lg' | 'xl'>('md');

  protected readonly avatarClasses = computed(() => {
    const sizes = {
      sm: 'h-9 w-9 text-sm',
      md: 'h-10 w-10 text-sm',
      lg: 'h-14 w-14 text-base',
      xl: 'h-20 w-20 text-2xl',
    };

    return `grid shrink-0 place-items-center overflow-hidden rounded-full bg-brand-50 font-semibold text-brand-700 ring-1 ring-brand-100 ${sizes[this.size()]}`;
  });
}
