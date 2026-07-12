import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { TimeoutError, timeout } from 'rxjs';
import { Employee360Service } from '../services/employee-360.service';
import { Employee360Response } from '../models/employee-360.model';

@Component({
  selector: 'app-employee-360',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './employee-360.component.html',
  styleUrl: './employee-360.component.css',
})
export class Employee360Component implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly employee360Service = inject(Employee360Service);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly data = signal<Employee360Response | null>(null);

  ngOnInit(): void {
    this.fetchData();
  }

  fetchData(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading.set(true);
    this.error.set(null);

    this.employee360Service
      .getEmployee360(id)
      .pipe(timeout(30000))
      .subscribe({
        next: (response) => {
          response.openTasks.sort((a, b) => {
            if (a.dueDate === null && b.dueDate === null) return 0;
            if (a.dueDate === null) return 1;
            if (b.dueDate === null) return -1;
            return a.dueDate.localeCompare(b.dueDate);
          });
          this.data.set(response);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(this.mapError(err));
          this.loading.set(false);
        },
      });
  }

  retry(): void {
    this.fetchData();
  }

  isOverdue(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date(new Date().toDateString());
  }

  truncateNotes(notes: string, maxLength = 200): string {
    if (notes.length <= maxLength) return notes;
    return notes.substring(0, maxLength) + '\u2026';
  }

  private mapError(err: unknown): string {
    if (err instanceof TimeoutError) {
      return 'Request timed out. Please try again.';
    }
    const httpError = err as { status?: number };
    if (httpError.status === 404) {
      return 'Employee not found';
    }
    if (httpError.status === 0) {
      return 'Request timed out. Please try again.';
    }
    return 'An error occurred while loading data. Please try again.';
  }
}
