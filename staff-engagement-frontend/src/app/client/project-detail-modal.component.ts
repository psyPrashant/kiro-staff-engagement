import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ModalComponent, ToastService } from '../shared';
import { ProjectService } from '../shared/services/project.service';
import { CompanyService } from '../shared/services/company.service';
import { Company } from '../shared/models/company.model';
import { ProjectDetail } from '../shared/models/project.model';

@Component({
  selector: 'app-project-detail-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './project-detail-modal.component.html',
  styleUrl: './client-modal.css',
})
export class ProjectDetailModalComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly companyService = inject(CompanyService);
  private readonly toast = inject(ToastService);

  readonly projectId = input.required<number>();

  readonly closed = output<void>();
  readonly updated = output<void>();

  readonly detail = signal<ProjectDetail | null>(null);
  readonly companies = signal<Company[]>([]);
  readonly loading = signal(true);
  readonly editing = signal(false);
  readonly submitting = signal(false);

  readonly editForm = new FormGroup({
    name: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(255)],
    }),
    companyId: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  ngOnInit(): void {
    this.loadDetail();
    this.companyService.getAll().subscribe({
      next: (companies) => this.companies.set(companies),
    });
  }

  private loadDetail(): void {
    this.loading.set(true);
    this.projectService.getDetail(this.projectId()).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load project');
        this.loading.set(false);
        this.closed.emit();
      },
    });
  }

  startEdit(): void {
    const current = this.detail();
    if (!current) return;
    this.editForm.setValue({
      name: current.name,
      companyId: String(current.companyId),
    });
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
  }

  editInvalid(field: 'name' | 'companyId'): boolean {
    const control = this.editForm.controls[field];
    return control.invalid && control.touched;
  }

  onSave(): void {
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid) {
      return;
    }

    const { name, companyId } = this.editForm.getRawValue();
    this.submitting.set(true);
    this.projectService
      .update(this.projectId(), { name: name.trim(), companyId: Number(companyId) })
      .subscribe({
        next: (detail) => {
          this.submitting.set(false);
          this.detail.set(detail);
          this.editing.set(false);
          this.toast.success('Project updated');
          this.updated.emit();
        },
        error: () => {
          this.submitting.set(false);
          this.toast.error('Failed to update project');
        },
      });
  }
}
