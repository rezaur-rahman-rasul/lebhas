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

@Component({
  selector: 'app-dialog, app-modal',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './app-dialog.html',
  styleUrl: './app-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalComponent {
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');
  private readonly modalId = `modal-${Math.random().toString(36).slice(2, 10)}`;

  readonly open = input(false);
  readonly title = input('');
  readonly description = input('');
  readonly ariaLabel = input('Dialog');
  readonly closeOnBackdrop = input(true);
  readonly closeOnEscape = input(true);
  readonly testId = input('');
  readonly panelClass = input('');
  readonly contentClass = input('');
  readonly closed = output<void>();

  protected readonly titleId = `${this.modalId}-title`;
  protected readonly descriptionId = `${this.modalId}-description`;
  protected readonly panelClasses = computed(() =>
    [
      'relative z-10 flex max-h-[calc(100dvh-1rem)] w-full flex-col overflow-hidden rounded-2xl border border-border bg-surface-elevated shadow-panel outline-none sm:max-h-[calc(100dvh-2rem)]',
      this.panelClass(),
    ]
      .filter(Boolean)
      .join(' '),
  );
  protected readonly contentPaddingClasses = computed(() =>
    [
      this.title() || this.description()
        ? 'min-h-0 overflow-y-auto p-4 sm:p-6'
        : 'min-h-0 overflow-y-auto p-4 sm:p-6',
      this.contentClass(),
    ]
      .filter(Boolean)
      .join(' '),
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
  protected onDocumentKeydown(event: KeyboardEvent): void {
    if (!this.open()) {
      return;
    }

    if (event.key === 'Escape' && this.closeOnEscape()) {
      event.preventDefault();
      this.closed.emit();
      return;
    }

    if (event.key !== 'Tab') {
      return;
    }

    const panel = this.panel()?.nativeElement;
    if (!panel) {
      return;
    }

    const focusableElements = this.getFocusableElements(panel);
    if (focusableElements.length === 0) {
      event.preventDefault();
      panel.focus();
      return;
    }

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];
    const activeElement =
      this.document.activeElement instanceof HTMLElement ? this.document.activeElement : null;

    if (event.shiftKey && activeElement === firstElement) {
      event.preventDefault();
      lastElement.focus();
    } else if (!event.shiftKey && activeElement === lastElement) {
      event.preventDefault();
      firstElement.focus();
    }
  }

  protected closeFromBackdrop(): void {
    if (this.closeOnBackdrop()) {
      this.closed.emit();
    }
  }

  private focusInitialElement(): void {
    if (!this.open()) {
      return;
    }

    const panel = this.panel()?.nativeElement;
    if (!panel) {
      return;
    }

    const initialFocusTarget =
      panel.querySelector<HTMLElement>('[data-modal-primary]') ??
      this.getFocusableElements(panel)[0];

    (initialFocusTarget ?? panel).focus();
  }

  private getFocusableElements(container: HTMLElement): HTMLElement[] {
    return Array.from(
      container.querySelectorAll<HTMLElement>(
        'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])',
      ),
    ).filter((element) => !element.hasAttribute('aria-hidden'));
  }

  private restoreFocus(): void {
    this.previousActiveElement?.focus();
    this.previousActiveElement = null;
  }
}
