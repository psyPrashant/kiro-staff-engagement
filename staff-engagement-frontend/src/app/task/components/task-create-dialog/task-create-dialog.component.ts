import {
  Component,
  ElementRef,
  HostListener,
  Input,
  effect,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';

import { TaskFormComponent } from '../task-form/task-form.component';
import { CreateTaskRequest } from '../../models/task.model';

@Component({
  selector: 'app-task-create-dialog',
  standalone: true,
  imports: [TaskFormComponent],
  templateUrl: './task-create-dialog.component.html',
  styleUrl: './task-create-dialog.component.css',
})
export class TaskCreateDialogComponent {
  readonly isOpen = signal(false);

  @Input()
  set open(value: boolean) {
    this.isOpen.set(value);
  }

  readonly employeeId = input<number | null>(null);
  readonly defaultAssigneeId = input<number | null>(null);
  readonly interactions = input<{ id: number; label: string }[]>([]);

  readonly closed = output<void>();
  readonly submitted = output<CreateTaskRequest>();

  readonly dialogPanel = viewChild<ElementRef<HTMLElement>>('dialogPanel');

  private previouslyFocusedElement: HTMLElement | null = null;

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        this.onDialogOpen();
      }
    });
  }

  private onDialogOpen(): void {
    this.previouslyFocusedElement = document.activeElement as HTMLElement | null;

    // Use setTimeout to wait for the dialog to render
    setTimeout(() => {
      this.focusFirstElement();
    });
  }

  private focusFirstElement(): void {
    const panel = this.dialogPanel()?.nativeElement;
    if (!panel) return;

    const focusable = this.getFocusableElements(panel);
    if (focusable.length > 0) {
      focusable[0].focus();
    }
  }

  private getFocusableElements(container: HTMLElement): HTMLElement[] {
    const selector = [
      'a[href]',
      'button:not([disabled])',
      'input:not([disabled])',
      'select:not([disabled])',
      'textarea:not([disabled])',
      '[tabindex]:not([tabindex="-1"])',
    ].join(', ');

    return Array.from(container.querySelectorAll<HTMLElement>(selector));
  }

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (!this.isOpen()) return;

    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeDialog();
      return;
    }

    if (event.key === 'Tab') {
      this.trapFocus(event);
    }
  }

  private trapFocus(event: KeyboardEvent): void {
    const panel = this.dialogPanel()?.nativeElement;
    if (!panel) return;

    const focusable = this.getFocusableElements(panel);
    if (focusable.length === 0) return;

    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    if (event.shiftKey) {
      if (document.activeElement === first) {
        event.preventDefault();
        last.focus();
      }
    } else {
      if (document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  }

  closeDialog(): void {
    this.closed.emit();
    this.returnFocus();
  }

  onFormSubmitted(request: CreateTaskRequest): void {
    this.submitted.emit(request);
  }

  onFormCancelled(): void {
    this.closeDialog();
  }

  onBackdropClick(): void {
    this.closeDialog();
  }

  private returnFocus(): void {
    if (this.previouslyFocusedElement) {
      this.previouslyFocusedElement.focus();
      this.previouslyFocusedElement = null;
    }
  }
}
