import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'user', pathMatch: 'full' },
  { path: 'user', loadChildren: () => import('./user/user.routes').then((m) => m.routes) },
  {
    path: 'employee',
    loadChildren: () => import('./employee/employee.routes').then((m) => m.routes),
  },
  { path: 'client', loadChildren: () => import('./client/client.routes').then((m) => m.routes) },
  {
    path: 'interaction',
    loadChildren: () => import('./interaction/interaction.routes').then((m) => m.routes),
  },
  { path: 'task', loadChildren: () => import('./task/task.routes').then((m) => m.routes) },
  { path: '**', redirectTo: 'user' },
];
