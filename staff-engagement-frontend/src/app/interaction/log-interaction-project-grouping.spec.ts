import * as fc from 'fast-check';
import { Project } from '../shared/models/project.model';

/**
 * Property 4: Project grouping preserves all items
 *
 * Validates: Requirements 7.2
 *
 * For any list of projects with companyName fields, grouping by company
 * and then flattening all groups SHALL produce a collection with the same
 * length and the same set of project IDs as the original list.
 */

/**
 * Groups projects by companyName into a Map — mirrors the computed signal
 * logic in LogInteractionComponent.projectsByCompany.
 */
function groupProjectsByCompany(projects: Project[]): Map<string, Project[]> {
  const groups = new Map<string, Project[]>();
  for (const p of projects) {
    const list = groups.get(p.companyName) ?? [];
    list.push(p);
    groups.set(p.companyName, list);
  }
  return groups;
}

describe('Property 4: Project grouping preserves all items', () => {
  const projectArb = fc.record({
    id: fc.nat(),
    name: fc.string({ minLength: 1, maxLength: 20 }),
    companyName: fc.string({ minLength: 1, maxLength: 20 }),
  });
  const projectListArb = fc.array(projectArb, { minLength: 0, maxLength: 50 });

  it('flattened count equals original count', () => {
    fc.assert(
      fc.property(projectListArb, (projects: Project[]) => {
        const grouped = groupProjectsByCompany(projects);

        // Flatten all groups
        const flattened: Project[] = [];
        for (const group of grouped.values()) {
          flattened.push(...group);
        }

        expect(flattened.length).toBe(projects.length);
      }),
      { numRuns: 100 },
    );
  });

  it('all IDs in flattened result are present in the original', () => {
    fc.assert(
      fc.property(projectListArb, (projects: Project[]) => {
        const grouped = groupProjectsByCompany(projects);

        // Flatten all groups
        const flattened: Project[] = [];
        for (const group of grouped.values()) {
          flattened.push(...group);
        }

        const originalIds = projects.map((p) => p.id);
        const flattenedIds = flattened.map((p) => p.id);

        // Every ID in the flattened result exists in the original
        for (const id of flattenedIds) {
          expect(originalIds).toContain(id);
        }

        // Every ID in the original exists in the flattened result
        for (const id of originalIds) {
          expect(flattenedIds).toContain(id);
        }
      }),
      { numRuns: 100 },
    );
  });
});
