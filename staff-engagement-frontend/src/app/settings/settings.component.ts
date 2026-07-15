import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingsService } from './settings.service';
import { ToastService } from '../shared';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
})
export class SettingsComponent {
  private readonly settingsService = inject(SettingsService);
  private readonly toast = inject(ToastService);

  readonly atRiskDays = signal(this.settingsService.thresholds().atRiskDays);
  readonly overdueDays = signal(this.settingsService.thresholds().overdueDays);

  save(): void {
    const atRisk = this.atRiskDays();
    const overdue = this.overdueDays();

    if (atRisk < 1 || overdue < 1) {
      this.toast.error('Thresholds must be at least 1 day');
      return;
    }
    if (atRisk >= overdue) {
      this.toast.error('"At risk" must be less than "Overdue"');
      return;
    }

    this.settingsService.save({ atRiskDays: atRisk, overdueDays: overdue });
    this.toast.success('Settings saved');
  }
}
