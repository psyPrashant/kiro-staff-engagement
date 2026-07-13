import { Routes } from '@angular/router';
import { EmployeesListComponent } from './employee';
import { Employee360Component } from './employee-360/employee-360.component';

export const routes: Routes = [
  { path: '', component: EmployeesListComponent },
  { path: ':id', component: Employee360Component },
];
