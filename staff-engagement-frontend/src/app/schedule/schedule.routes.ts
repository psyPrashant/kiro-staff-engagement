import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./schedule-calendar/schedule-calendar.component').then(
        (m) => m.ScheduleCalendarComponent,
      ),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./schedule-form/schedule-form.component').then((m) => m.ScheduleFormComponent),
  },
];
