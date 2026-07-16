export enum TaskStatus {
  OPEN = 'OPEN',
  DONE = 'DONE',
}

export function formatTaskStatusLabel(status: TaskStatus): string {
  switch (status) {
    case TaskStatus.OPEN:
      return 'Open';
    case TaskStatus.DONE:
      return 'Done';
  }
}

export function taskStatusBadgeClass(status: TaskStatus): string {
  switch (status) {
    case TaskStatus.OPEN:
      return 'badge-warning';
    case TaskStatus.DONE:
      return 'badge-success';
  }
}
