export interface Employee360Response {
  profile: ProfileDto;
  interactions: InteractionDto[];
  openTasks: TaskDto[];
}

export interface ProfileDto {
  id: number;
  name: string;
  email: string;
  jobTitle: string;
  managerName: string | null;
}

export interface InteractionDto {
  id: number;
  type: string;
  occurredAt: string;
  conductedByName: string;
  notes: string;
  projectContext: ProjectContextDto | null;
}

export interface ProjectContextDto {
  projectName: string;
  companyName: string;
}

export interface TaskDto {
  id: number;
  title: string;
  dueDate: string | null;
  assignedUserName: string;
}
