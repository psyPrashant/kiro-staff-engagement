import { Component } from '@angular/core';
import { InteractionMatrixComponent } from './interaction-matrix/interaction-matrix.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [InteractionMatrixComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent {}
