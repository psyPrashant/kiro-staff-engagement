import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { EmployeeService } from '../shared/services/employee.service';
import { EngagementService } from '../dashboard/services/engagement.service';
import {
  EngagementStatus,
  MatrixEntry,
  formatEngagementStatusLabel,
} from '../dashboard/models/engagement.model';
import { Employee } from '../shared/models/employee.model';
import { EmployeeListEntry } from './models/employee-list.model';
import { AvatarComponent, PaginationComponent, ToastService } from '../shared';
import { EmployeeCreateModalComponent } from './employee-create-modal.component';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-employee',
  standalone: true,
  imports: [AvatarComponent, PaginationComponent, EmployeeCreateModalComponent],
  templateUrl: './employee.html',
  styleUrl: './employee.css',
})
export class EmployeesListComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly engagementService = inject(EngagementService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  // Expose enum for template use
  readonly EngagementStatus = EngagementStatus;

  readonly employees = signal<EmployeeListEntry[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly searchTerm = signal('');
  readonly statusFilter = signal<EngagementStatus | null>(null);
  readonly showCreateModal = signal<boolean>(false);
  readonly page = signal<number>(1);

  readonly filteredEmployees = computed(() => {
    let result = this.employees();
    const term = this.searchTerm().toLowerCase();
    if (term) {
      result = result.filter(
        (e) => e.name.toLowerCase().includes(term) || e.jobTitle.toLowerCase().includes(term),
      );
    }
    const status = this.statusFilter();
    if (status) {
      result = result.filter((e) => e.engagementStatus === status);
    }
    return result;
  });

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredEmployees().length / PAGE_SIZE)),
  );

  readonly pagedEmployees = computed(() => {
    const start = (this.page() - 1) * PAGE_SIZE;
    return this.filteredEmployees().slice(start, start + PAGE_SIZE);
  });

  // Candidate managers for the create-employee modal.
  readonly managers = computed(() => this.employees().map((e) => ({ id: e.id, name: e.name })));

  constructor() {
    // Reset to the first page whenever the active filters change.
    effect(() => {
      this.searchTerm();
      this.statusFilter();
      this.page.set(1);
    });
  }

  ngOnInit(): void {
    this.fetchData();
  }

  onEmployeeCreated(): void {
    this.showCreateModal.set(false);
    this.toast.success('Employee added');
    this.fetchData();
  }

  fetchData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      employees: this.employeeService.getAll(),
      matrix: this.engagementService.getMatrix(),
    }).subscribe({
      next: ({ employees, matrix }) => {
        this.employees.set(this.joinData(employees, matrix));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load employees. Please try again.');
        this.loading.set(false);
      },
    });
  }

  navigateToEmployee(id: number): void {
    this.router.navigate(['/employee', id]);
  }

  getInitials(name: string): string {
    const parts = name.trim().split(/\s+/);
    const first = parts[0]?.charAt(0).toUpperCase() ?? '';
    const last = parts.length > 1 ? parts[parts.length - 1].charAt(0).toUpperCase() : '';
    return first + last;
  }

  formatStatusLabel(status: EngagementStatus): string {
    return formatEngagementStatusLabel(status);
  }

  getBadgeClass(status: EngagementStatus): string {
    switch (status) {
      case EngagementStatus.OVERDUE:
        return 'badge badge-danger';
      case EngagementStatus.AT_RISK:
        return 'badge badge-warning';
      case EngagementStatus.ON_TRACK:
        return 'badge badge-success';
      default:
        return 'badge';
    }
  }

  private joinData(employees: Employee[], matrix: MatrixEntry[]): EmployeeListEntry[] {
    const matrixMap = new Map<number, MatrixEntry>();
    for (const entry of matrix) {
      matrixMap.set(entry.employeeId, entry);
    }

    return employees.map((emp) => {
      const engagement = matrixMap.get(emp.id);
      return {
        id: emp.id,
        name: emp.name,
        email: emp.email,
        jobTitle: emp.jobTitle,
        managerName: emp.manager?.name ?? null,
        engagementStatus: engagement?.engagementStatus ?? null,
        lastInteractionDate: engagement?.lastInteractionDate ?? null,
      };
    });
  }
}
