import { Component, inject, OnInit, signal } from '@angular/core';
import { ProjectService } from '../shared/services/project.service';
import { ProjectSummary } from '../shared/models/project.model';
import { CreateProjectModalComponent } from './create-project-modal.component';
import { ProjectDetailModalComponent } from './project-detail-modal.component';

@Component({
  selector: 'app-client',
  standalone: true,
  imports: [CreateProjectModalComponent, ProjectDetailModalComponent],
  templateUrl: './client.html',
  styleUrl: './client.css',
})
export class Client implements OnInit {
  private readonly projectService = inject(ProjectService);

  readonly projects = signal<ProjectSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showCreateModal = signal(false);
  readonly selectedProjectId = signal<number | null>(null);

  ngOnInit(): void {
    this.fetchData();
  }

  fetchData(): void {
    this.loading.set(true);
    this.error.set(null);
    this.projectService.getSummaries().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load companies. Please try again.');
        this.loading.set(false);
      },
    });
  }

  onProjectCreated(): void {
    this.showCreateModal.set(false);
    this.fetchData();
  }

  onProjectUpdated(): void {
    this.fetchData();
  }
}
