import { DOCUMENT } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  input,
  output,
  viewChild,
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
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');
  private readonly drawerId = `drawer-${Math.random().toString(36).slice(2, 10)}`;

  readonly open = input(false);
  readonly title = input('');
  readonly description = input('');
  readonly ariaLabel = input('Drawer');
  readonly side = input<DrawerSide>('right');
  readonly closed = output<void>();

  protected readonly titleId = `${this.drawerId}-title`;
  protected readonly descriptionId = `${this.drawerId}-description`;
  protected readonly panelClasses = computed(() =>
    [
      'fixed inset-y-0 z-50 flex w-full max-w-xl flex-col border-border bg-surface-elevated shadow-panel outline-none transition sm:max-w-xl',
      this.side() === 'right' ? 'right-0 border-l' : 'left-0 border-r',
    ].join(' '),
  );

  private previousActiveElement: HTMLElement | null = null;
  private wasOpen = false;

  constructor() {
    effect(() => {
      const isOpen = this.open();
      if (isOpen && !this.wasOpen) {
        this.previousActiveElement =
          this.document.activeElement instanceof HTMLElement ? this.document.activeElement : null;
        this.document.body.style.overflow = 'hidden';
        queueMicrotask(() => this.focusInitialElement());
      } else if (!isOpen && this.wasOpen) {
        this.document.body.style.overflow = '';
        this.restoreFocus();
      }
      this.wasOpen = isOpen;
    });

    this.destroyRef.onDestroy(() => {
      this.document.body.style.overflow = '';
      this.restoreFocus();
    });
  }

  @HostListener('document:keydown', ['$event'])
  protected onEscape(event: KeyboardEvent): void {
    if (!this.open() || event.key !== 'Escape') {
      return;
    }

    event.preventDefault();
    this.closed.emit();
  }

  private focusInitialElement(): void {
    if (!this.open()) {
      return;
    }

    const panel = this.panel()?.nativeElement;
    const firstFocusable = panel?.querySelector<HTMLElement>(
      'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])',
    );

    (firstFocusable ?? panel)?.focus();
  }

  private restoreFocus(): void {
    this.previousActiveElement?.focus();
    this.previousActiveElement = null;
  }
}
