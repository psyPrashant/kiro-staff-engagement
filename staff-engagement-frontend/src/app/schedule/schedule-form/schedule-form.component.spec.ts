import '@angular/compiler';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Location } from '@angular/common';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ScheduleFormComponent } from './schedule-form.component';
import { SchedulingService } from '../services/scheduling.service';
import { ScheduledInteraction } from '../models/scheduled-interaction.model';

function todayString(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function pastDateString(): string {
  const past = new Date();
  past.setDate(past.getDate() - 5);
  const year = past.getFullYear();
  const month = String(past.getMonth() + 1).padStart(2, '0');
  const day = String(past.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function futureDateString(): string {
  const future = new Date();
  future.setDate(future.getDate() + 7);
  const year = future.getFullYear();
  const month = String(future.getMonth() + 1).padStart(2, '0');
  const day = String(future.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const mockScheduledInteraction: ScheduledInteraction = {
  id: 1,
  employeeId: 42,
  employeeName: 'John Doe',
  scheduledDate: futureDateString(),
  interactionType: 'CHECK_IN',
  completionStatus: 'PENDING',
  notes: null,
  overdue: false,
  createdAt: '2025-01-20T10:30:00Z',
};

describe('ScheduleFormComponent', () => {
  let component: ScheduleFormComponent;
  let fixture: ComponentFixture<ScheduleFormComponent>;
  let mockSchedulingService: { create: ReturnType<typeof vi.fn> };
  let mockLocation: { back: ReturnType<typeof vi.fn> };

  function createComponent(employeeId: string | null = '42') {
    mockSchedulingService = {
      create: vi.fn().mockReturnValue(of(mockScheduledInteraction)),
    };

    mockLocation = {
      back: vi.fn(),
    };

    const mockActivatedRoute = {
      snapshot: {
        queryParamMap: convertToParamMap(
          employeeId ? { employeeId } : {}
        ),
      },
    };

    TestBed.configureTestingModule({
      imports: [ScheduleFormComponent],
      providers: [
        { provide: SchedulingService, useValue: mockSchedulingService },
        { provide: Location, useValue: mockLocation },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
      ],
    });

    fixture = TestBed.createComponent(ScheduleFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  describe('Date Validation', () => {
    beforeEach(() => {
      createComponent('42');
    });

    it('should show date validation error when a past date is entered', () => {
      const pastDate = pastDateString();
      component.form.controls.scheduledDate.setValue(pastDate);
      component.onDateChange();
      fixture.detectChanges();

      expect(component.dateError()).toBeTruthy();

      const compiled = fixture.nativeElement as HTMLElement;
      const errorEl = compiled.querySelector('[data-testid="date-validation-error"]');
      expect(errorEl).not.toBeNull();
      expect(errorEl!.textContent).toContain('today or in the future');
    });

    it('should NOT show date validation error when today is entered', () => {
      const today = todayString();
      component.form.controls.scheduledDate.setValue(today);
      component.onDateChange();
      fixture.detectChanges();

      expect(component.dateError()).toBeNull();

      const compiled = fixture.nativeElement as HTMLElement;
      const errorEl = compiled.querySelector('[data-testid="date-validation-error"]');
      expect(errorEl).toBeNull();
    });

    it('should NOT show date validation error when a future date is entered', () => {
      const future = futureDateString();
      component.form.controls.scheduledDate.setValue(future);
      component.onDateChange();
      fixture.detectChanges();

      expect(component.dateError()).toBeNull();

      const compiled = fixture.nativeElement as HTMLElement;
      const errorEl = compiled.querySelector('[data-testid="date-validation-error"]');
      expect(errorEl).toBeNull();
    });
  });

  describe('Submit Button Disabled State', () => {
    beforeEach(() => {
      createComponent('42');
    });

    it('should disable submit button when date is a past date', () => {
      component.form.controls.scheduledDate.setValue(pastDateString());
      component.onDateChange();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const submitBtn = compiled.querySelector(
        '[data-testid="submit-btn"]'
      ) as HTMLButtonElement;
      expect(submitBtn.disabled).toBe(true);
    });

    it('should disable submit button when date is empty', () => {
      component.form.controls.scheduledDate.setValue('');
      component.onDateChange();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const submitBtn = compiled.querySelector(
        '[data-testid="submit-btn"]'
      ) as HTMLButtonElement;
      expect(submitBtn.disabled).toBe(true);
    });

    it('should enable submit button when form is valid with a future date', () => {
      component.form.controls.scheduledDate.setValue(futureDateString());
      component.form.controls.interactionType.setValue('CHECK_IN');
      component.onDateChange();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const submitBtn = compiled.querySelector(
        '[data-testid="submit-btn"]'
      ) as HTMLButtonElement;
      expect(submitBtn.disabled).toBe(false);
    });
  });

  describe('Successful Form Submission', () => {
    beforeEach(() => {
      createComponent('42');
    });

    it('should call Location.back() and show success notification on successful submission', () => {
      component.form.controls.scheduledDate.setValue(futureDateString());
      component.form.controls.interactionType.setValue('CHECK_IN');
      component.onDateChange();
      fixture.detectChanges();

      component.submit();
      fixture.detectChanges();

      expect(mockLocation.back).toHaveBeenCalled();
      expect(component.successNotification()).toBe(true);

      const compiled = fixture.nativeElement as HTMLElement;
      const successEl = compiled.querySelector('[data-testid="success-notification"]');
      expect(successEl).not.toBeNull();
      expect(successEl!.textContent).toContain('successfully');
    });
  });

  describe('API Error Handling', () => {
    beforeEach(() => {
      createComponent('42');
    });

    it('should display API error message without navigating on submission error', () => {
      mockSchedulingService.create.mockReturnValue(
        throwError(() => ({
          error: { message: 'Employee not found' },
        }))
      );

      component.form.controls.scheduledDate.setValue(futureDateString());
      component.form.controls.interactionType.setValue('CHECK_IN');
      component.onDateChange();
      fixture.detectChanges();

      component.submit();
      fixture.detectChanges();

      expect(mockLocation.back).not.toHaveBeenCalled();
      expect(component.apiError()).toBe('Employee not found');

      const compiled = fixture.nativeElement as HTMLElement;
      const errorEl = compiled.querySelector('[data-testid="api-error"]');
      expect(errorEl).not.toBeNull();
      expect(errorEl!.textContent).toContain('Employee not found');
    });

    it('should display fallback error message when API returns no message', () => {
      mockSchedulingService.create.mockReturnValue(
        throwError(() => ({ error: {} }))
      );

      component.form.controls.scheduledDate.setValue(futureDateString());
      component.form.controls.interactionType.setValue('CHECK_IN');
      component.onDateChange();
      fixture.detectChanges();

      component.submit();
      fixture.detectChanges();

      expect(component.apiError()).toBe('Failed to schedule interaction. Please try again.');

      const compiled = fixture.nativeElement as HTMLElement;
      const errorEl = compiled.querySelector('[data-testid="api-error"]');
      expect(errorEl).not.toBeNull();
    });
  });
});
