import {
  Component,
  ElementRef,
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
        tabindex="-1"
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

  // Note: Escape does NOT close the modal. Per the app-wide modal rule, a modal
  // only closes when the backdrop (area outside the dialog) is clicked or when a
  // relevant action button inside the modal is used.

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }

  private trapFocus(): void {
    const el = this.dialog().nativeElement;
    // Focus the first form control inside the projected content — NOT the close
    // button. Auto-focusing a <button> means Space/Enter would activate it (e.g.
    // the × close button), unexpectedly dismissing the modal.
    const firstField = el.querySelector<HTMLElement>(
      'input, select, textarea, [href], [tabindex]:not([tabindex="-1"])',
    );
    if (firstField) {
      firstField.focus();
    } else {
      // No form control present — focus the dialog container itself so focus
      // stays within the modal without triggering any button.
      el.focus();
    }
  }
}
