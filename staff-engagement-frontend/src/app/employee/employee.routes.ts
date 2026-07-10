import { Routes } from '@angular/router';
import { Employee } from './employee';
import { Employee360Component } from './employee-360/employee-360.component';

export const routes: Routes = [
  { path: '', component: Employee },
  { path: ':id', component: Employee360Component },
];
