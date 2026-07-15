import { Injectable, signal } from '@angular/core';

export interface EngagementThresholds {
  atRiskDays: number;
  overdueDays: number;
}

const STORAGE_KEY = 'engagement_thresholds';
const DEFAULTS: EngagementThresholds = { atRiskDays: 14, overdueDays: 30 };

@Injectable({ providedIn: 'root' })
export class SettingsService {
  readonly thresholds = signal<EngagementThresholds>(this.load());

  save(thresholds: EngagementThresholds): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(thresholds));
    this.thresholds.set(thresholds);
  }

  private load(): EngagementThresholds {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        return JSON.parse(stored) as EngagementThresholds;
      } catch {
        return DEFAULTS;
      }
    }
    return DEFAULTS;
  }
}
