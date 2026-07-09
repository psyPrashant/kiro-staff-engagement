import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login.component').then((m) => m.LoginComponent),
  },
  { path: '', redirectTo: 'user', pathMatch: 'full' },
  {
    path: 'user',
    loadChildren: () => import('./user/user.routes').then((m) => m.routes),
    canActivate: [authGuard],
  },
  {
    path: 'employee',
    loadChildren: () => import('./employee/employee.routes').then((m) => m.routes),
    canActivate: [authGuard],
  },
  {
    path: 'client',
    loadChildren: () => import('./client/client.routes').then((m) => m.routes),
    canActivate: [authGuard],
  },
  {
    path: 'interaction',
    loadChildren: () => import('./interaction/interaction.routes').then((m) => m.routes),
    canActivate: [authGuard],
  },
  {
    path: 'task',
    loadChildren: () => import('./task/task.routes').then((m) => m.routes),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: 'user' },
];
