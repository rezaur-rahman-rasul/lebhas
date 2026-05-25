import { DOCUMENT } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  output,
} from '@angular/core';

import { IconComponent } from '../icon/icon';

type DrawerSide = 'right' | 'left';

@Component({
  selector: 'app-drawer',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './app-drawer.html',
  styleUrl: './app-drawer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppDrawerComponent {
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  readonly open = input(false);
  readonly title = input('');
  readonly description = input('');
  readonly ariaLabel = input('Drawer');
  readonly side = input<DrawerSide>('right');
  readonly closed = output<void>();

  protected readonly panelClasses = computed(() =>
    [
      'fixed inset-y-0 z-50 flex w-full max-w-xl flex-col border-border bg-surface-elevated shadow-panel outline-none transition',
      this.side() === 'right' ? 'right-0 border-l' : 'left-0 border-r',
    ].join(' '),
  );

  private wasOpen = false;

  constructor() {
    effect(() => {
      const isOpen = this.open();
      if (isOpen && !this.wasOpen) {
        this.document.body.style.overflow = 'hidden';
      } else if (!isOpen && this.wasOpen) {
        this.document.body.style.overflow = '';
      }
      this.wasOpen = isOpen;
    });

    this.destroyRef.onDestroy(() => {
      this.document.body.style.overflow = '';
    });
  }
}
