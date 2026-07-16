export interface Project {
  id: number;
  name: string;
  company: {
    id: number;
    name: string;
  };
}

/** Row model for the Companies/Projects list view. */
export interface ProjectSummary {
  id: number;
  name: string;
  companyId: number;
  companyName: string;
  employeeCount: number;
}

/** An employee associated with a project (via their interactions). */
export interface AssignedEmployee {
  id: number;
  name: string;
}

/** Detail model for a single project. */
export interface ProjectDetail {
  id: number;
  name: string;
  companyId: number;
  companyName: string;
  employees: AssignedEmployee[];
}

export interface CreateProjectRequest {
  name: string;
  /** Provide an existing company... */
  companyId?: number | null;
  /** ...or a new company name to create inline. */
  newCompanyName?: string | null;
}

export interface UpdateProjectRequest {
  name: string;
  companyId?: number | null;
}
