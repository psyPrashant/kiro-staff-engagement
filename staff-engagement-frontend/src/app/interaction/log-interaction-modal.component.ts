import { Component, input, output } from '@angular/core';
import { ModalComponent } from '../shared';
import { LogInteractionComponent } from './log-interaction.component';

@Component({
  selector: 'app-log-interaction-modal',
  standalone: true,
  imports: [ModalComponent, LogInteractionComponent],
  template: `
    <app-modal title="Log interaction" (close)="closed.emit()">
      <app-log-interaction />
    </app-modal>
  `,
})
export class LogInteractionModalComponent {
  readonly prefilledEmployeeId = input<number | null>(null);
  readonly closed = output<void>();
}
