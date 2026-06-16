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
  signal,
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
  private readonly overlay = viewChild<ElementRef<HTMLElement>>('overlay');
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
  protected readonly hasAdminModalForm = signal(false);
  protected readonly panelClasses = computed(() =>
    [
      'relative z-10 flex h-[calc(100dvh-32px)] max-h-[calc(100dvh-32px)] w-full flex-col overflow-hidden rounded-2xl border border-border bg-surface-elevated shadow-panel outline-none',
      'max-w-[48rem]',
      this.panelClass(),
    ]
      .filter(Boolean)
      .join(' '),
  );
  protected readonly contentPaddingClasses = computed(() =>
    [
      this.hasAdminModalForm()
        ? 'app-dialog-content app-dialog-content--admin-form min-h-0 flex-1 overflow-hidden !p-0'
        : 'app-dialog-content min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-4 pb-6 sm:px-6 sm:py-6 sm:pb-6',
      this.contentClass(),
    ]
      .filter(Boolean)
      .join(' '),
  );

  private previousActiveElement: HTMLElement | null = null;
  private wasOpen = false;
  private lockedScrollY = 0;
  private previousHtmlOverflow = '';
  private previousBodyOverflow = '';
  private previousBodyPosition = '';
  private previousBodyTop = '';
  private previousBodyWidth = '';
  private attachedOverlay: HTMLElement | null = null;

  constructor() {
    effect(() => {
      const isOpen = this.open();

      if (isOpen && !this.wasOpen) {
        this.previousActiveElement =
          this.document.activeElement instanceof HTMLElement ? this.document.activeElement : null;
        this.lockDocumentScroll();
        queueMicrotask(() => {
          this.attachOverlayToDocumentBody();
          this.updateProjectedContentMode();
          this.focusInitialElement();
        });
      } else if (!isOpen && this.wasOpen) {
        this.hasAdminModalForm.set(false);
        this.detachOverlayFromDocumentBody();
        this.unlockDocumentScroll();
        this.restoreFocus();
      }

      this.wasOpen = isOpen;
    });

    this.destroyRef.onDestroy(() => {
      this.detachOverlayFromDocumentBody();
      this.unlockDocumentScroll();
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

  private updateProjectedContentMode(): void {
    const panel = this.panel()?.nativeElement;
    this.hasAdminModalForm.set(!!panel?.querySelector('.admin-modal-form'));
  }

  private attachOverlayToDocumentBody(): void {
    const overlay = this.overlay()?.nativeElement;
    if (!overlay || overlay.parentElement === this.document.body) {
      return;
    }

    this.document.body.appendChild(overlay);
    this.attachedOverlay = overlay;
  }

  private detachOverlayFromDocumentBody(): void {
    if (this.attachedOverlay?.parentElement === this.document.body) {
      this.document.body.removeChild(this.attachedOverlay);
    }
    this.attachedOverlay = null;
  }

  private lockDocumentScroll(): void {
    const html = this.document.documentElement;
    const body = this.document.body;

    this.lockedScrollY = globalThis.scrollY || html.scrollTop || body.scrollTop || 0;
    this.previousHtmlOverflow = html.style.overflow;
    this.previousBodyOverflow = body.style.overflow;
    this.previousBodyPosition = body.style.position;
    this.previousBodyTop = body.style.top;
    this.previousBodyWidth = body.style.width;

    html.classList.add('app-modal-open', 'lebhas-modal-open');
    body.classList.add('app-modal-open', 'lebhas-modal-open');
    html.style.overflow = 'hidden';
    body.style.overflow = 'hidden';
    body.style.position = 'fixed';
    body.style.top = `-${this.lockedScrollY}px`;
    body.style.width = '100%';
  }

  private unlockDocumentScroll(): void {
    const html = this.document.documentElement;
    const body = this.document.body;

    html.classList.remove('app-modal-open', 'lebhas-modal-open');
    body.classList.remove('app-modal-open', 'lebhas-modal-open');
    html.style.overflow = this.previousHtmlOverflow;
    body.style.overflow = this.previousBodyOverflow;
    body.style.position = this.previousBodyPosition;
    body.style.top = this.previousBodyTop;
    body.style.width = this.previousBodyWidth;

    if (this.lockedScrollY > 0) {
      globalThis.scrollTo(0, this.lockedScrollY);
    }
    this.lockedScrollY = 0;
  }
}
