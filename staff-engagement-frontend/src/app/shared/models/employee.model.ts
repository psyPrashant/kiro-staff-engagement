export interface Employee {
  id: number;
  name: string;
  email: string;
  jobTitle: string;
  manager?: { id: number; name: string } | null;
}
