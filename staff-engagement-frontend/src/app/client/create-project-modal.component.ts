import { Component, inject, OnInit, output, signal } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ModalComponent, ToastService } from '../shared';
import { ProjectService } from '../shared/services/project.service';
import { CompanyService } from '../shared/services/company.service';
import { Company } from '../shared/models/company.model';
import { CreateProjectRequest } from '../shared/models/project.model';

@Component({
  selector: 'app-create-project-modal',
  standalone: true,
  imports: [ReactiveFormsModule, ModalComponent],
  templateUrl: './create-project-modal.component.html',
  styleUrl: './client-modal.css',
})
export class CreateProjectModalComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly companyService = inject(CompanyService);
  private readonly toast = inject(ToastService);

  readonly closed = output<void>();
  readonly created = output<void>();

  readonly companies = signal<Company[]>([]);
  readonly isNewCompany = signal(false);
  readonly submitting = signal(false);

  readonly form = new FormGroup({
    name: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(255)],
    }),
    company: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    newCompanyName: new FormControl<string>('', { nonNullable: true }),
  });

  ngOnInit(): void {
    this.companyService.getAll().subscribe({
      next: (companies) => this.companies.set(companies),
    });
  }

  onCompanyChange(): void {
    const isNew = this.form.controls.company.value === 'new';
    this.isNewCompany.set(isNew);

    const newCompanyName = this.form.controls.newCompanyName;
    if (isNew) {
      newCompanyName.setValidators([Validators.required, Validators.maxLength(255)]);
    } else {
      newCompanyName.clearValidators();
      newCompanyName.setValue('');
    }
    newCompanyName.updateValueAndValidity();
  }

  isInvalid(field: 'name' | 'company' | 'newCompanyName'): boolean {
    const control = this.form.controls[field];
    return control.invalid && control.touched;
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    const { name, company, newCompanyName } = this.form.getRawValue();
    const request: CreateProjectRequest =
      company === 'new'
        ? { name: name.trim(), newCompanyName: newCompanyName.trim() }
        : { name: name.trim(), companyId: Number(company) };

    this.submitting.set(true);
    this.projectService.create(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Project created');
        this.created.emit();
      },
      error: () => {
        this.submitting.set(false);
        this.toast.error('Failed to create project');
      },
    });
  }
}
