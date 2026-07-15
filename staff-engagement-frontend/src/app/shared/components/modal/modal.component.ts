import {
  Component,
  ElementRef,
  HostListener,
  input,
  output,
  AfterViewInit,
  OnDestroy,
  viewChild,
} from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  template: `
    <div class="modal-backdrop" (click)="onBackdropClick($event)">
      <div
        class="modal-dialog"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="title()"
        #dialog
      >
        <div class="modal-header">
          <h2>{{ title() }}</h2>
          <button class="modal-close" (click)="close.emit()" aria-label="Close">&times;</button>
        </div>
        <ng-content />
      </div>
    </div>
  `,
})
export class ModalComponent implements AfterViewInit, OnDestroy {
  readonly title = input.required<string>();
  readonly close = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLElement>>('dialog');
  private previousFocus: HTMLElement | null = null;

  ngAfterViewInit(): void {
    this.previousFocus = document.activeElement as HTMLElement;
    this.trapFocus();
  }

  ngOnDestroy(): void {
    this.previousFocus?.focus();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close.emit();
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }

  private trapFocus(): void {
    const el = this.dialog().nativeElement;
    const focusable = el.querySelector<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
    );
    focusable?.focus();
  }
}
