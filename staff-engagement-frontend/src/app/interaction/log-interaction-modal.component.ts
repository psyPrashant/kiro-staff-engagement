import { Component, input, output } from '@angular/core';
import { ModalComponent } from '../shared';
import { LogInteractionComponent } from './log-interaction.component';

@Component({
  selector: 'app-log-interaction-modal',
  standalone: true,
  imports: [ModalComponent, LogInteractionComponent],
  template: `
    <app-modal title="Log interaction" (close)="closed.emit()">
      <app-log-interaction
        [embedded]="true"
        [prefilledEmployeeId]="prefilledEmployeeId()"
        (saved)="saved.emit()"
        (cancelled)="closed.emit()"
      />
    </app-modal>
  `,
})
export class LogInteractionModalComponent {
  readonly prefilledEmployeeId = input<number | null>(null);
  readonly closed = output<void>();
  readonly saved = output<void>();
}
