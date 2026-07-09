import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { ShellComponent } from './shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'user',
        loadChildren: () => import('./user/user.routes').then((m) => m.routes),
      },
      {
        path: 'employee',
        loadChildren: () => import('./employee/employee.routes').then((m) => m.routes),
      },
      {
        path: 'client',
        loadChildren: () => import('./client/client.routes').then((m) => m.routes),
      },
      {
        path: 'interaction',
        loadChildren: () => import('./interaction/interaction.routes').then((m) => m.routes),
      },
      {
        path: 'task',
        loadChildren: () => import('./task/task.routes').then((m) => m.routes),
      },
      { path: '**', redirectTo: 'dashboard' },
    ],
  },
];
