# Implementation Plan: Dashboard Interaction Matrix

## Overview

Replace the placeholder skeleton cards on the dashboard with a fully functional engagement interaction matrix. Implementation follows the Angular 21 standalone component pattern with signals, creates a feature-local engagement service, and adds child components for filtering, sorting, and follow-up display. All new code lives under `src/app/dashboard/` in the frontend project.

## Tasks

- [x] 1. Create data models and engagement service
  - [x] 1.1 Create TypeScript interfaces and types
    - Create `src/app/dashboard/models/engagement.model.ts`
    - Define `EngagementStatus` type (`'OVERDUE' | 'AT_RISK' | 'ON_TRACK'`)
    - Define `SortOption` type (`'name' | 'recency'`)
    - Define `MatrixEntry` interface with all fields matching the backend DTO
    - _Requirements: 1.5, 1.6_

  - [x] 1.2 Implement EngagementService
    - Create `src/app/dashboard/services/engagement.service.ts`
    - Injectable service provided in root, using `inject(HttpClient)`
    - Implement `getMatrix(params?: { status?: EngagementStatus; sort?: SortOption }): Observable<MatrixEntry[]>`
    - Conditionally construct query parameters: no params when absent, `?status=X` when status provided, `?sort=X` when sort provided, both when both provided
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.3 Write unit tests for EngagementService
    - Create `src/app/dashboard/services/engagement.service.spec.ts`
    - Verify GET to `/api/engagement/matrix` with no params
    - Verify GET with `?status=OVERDUE` when status filter provided
    - Verify GET with `?sort=recency` when sort provided
    - Verify GET with both `?status=AT_RISK&sort=recency` when both provided
    - _Requirements: 8.1_

  - [x] 1.4 Write property test for service URL construction
    - **Property 1: Service URL construction**
    - Use fast-check to generate all combinations of optional status and sort values
    - Assert constructed URL matches `/api/engagement/matrix` with only non-undefined params as query params
    - **Validates: Requirements 1.2, 1.3, 1.4, 6.5**

- [x] 2. Implement StatusFilter and SortControl child components
  - [x] 2.1 Implement StatusFilterComponent
    - Create `src/app/dashboard/status-filter/status-filter.component.ts` (standalone)
    - Create `src/app/dashboard/status-filter/status-filter.component.html`
    - Create `src/app/dashboard/status-filter/status-filter.component.css`
    - Accept `activeFilter` as input signal, emit `filterChange` output
    - Render filter buttons for All, OVERDUE, AT_RISK, ON_TRACK with active state styling
    - Ensure keyboard accessibility (buttons are natively focusable)
    - _Requirements: 5.1, 5.4, 7.2_

  - [x] 2.2 Implement SortControlComponent
    - Create `src/app/dashboard/sort-control/sort-control.component.ts` (standalone)
    - Create `src/app/dashboard/sort-control/sort-control.component.html`
    - Create `src/app/dashboard/sort-control/sort-control.component.css`
    - Accept `activeSort` as input signal, emit `sortChange` output
    - Render toggle between "Name (A-Z)" and "Recency (longest gap first)" with active state
    - _Requirements: 6.1, 6.4, 7.2_

- [x] 3. Implement FollowUpSection child component
  - [x] 3.1 Implement FollowUpSectionComponent
    - Create `src/app/dashboard/follow-up-section/follow-up-section.component.ts` (standalone)
    - Create `src/app/dashboard/follow-up-section/follow-up-section.component.html`
    - Create `src/app/dashboard/follow-up-section/follow-up-section.component.css`
    - Accept `entries` input signal (pre-filtered to `followUpRequired === true`)
    - Display each entry with name, status badge, recency value
    - Render drill-through links to Employee 360 (`/employee/{id}`) and log interaction (`/interaction?employeeId={id}`)
    - Show "No follow-ups needed at this time" when entries array is empty
    - Style as a visually distinct card/panel
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 4. Implement InteractionMatrixComponent (orchestrator)
  - [x] 4.1 Create InteractionMatrixComponent with state management
    - Create `src/app/dashboard/interaction-matrix/interaction-matrix.component.ts` (standalone)
    - Create `src/app/dashboard/interaction-matrix/interaction-matrix.component.html`
    - Create `src/app/dashboard/interaction-matrix/interaction-matrix.component.css`
    - Define signals: `entries`, `loading`, `error`, `activeFilter`, `activeSort`
    - Implement `fetchMatrix()` method that calls EngagementService with current filter/sort state
    - Trigger initial fetch on component init
    - Handle loading state (show loading indicator with `aria-busy`)
    - Handle error state (show error message with retry button)
    - _Requirements: 2.1, 2.5, 2.6, 5.5, 7.5_

  - [x] 4.2 Implement matrix table rendering
    - Use semantic HTML: `<table>` with `<thead>` and `<tbody>`
    - Render one row per MatrixEntry: employee name, status badge, recency, frequency, last interaction date
    - Format recency: `null` → "No interactions", number → "{n} days"
    - Format lastInteractionDate: `null` → "Never", non-null → formatted date
    - Apply CSS classes per status: `status-overdue`, `status-at-risk`, `status-on-track`
    - Add `aria-label` on status badges (e.g., "Status: Overdue")
    - Show follow-up indicator when `followUpRequired` is true
    - _Requirements: 2.2, 2.3, 2.4, 7.1, 7.3, 7.4_

  - [x] 4.3 Implement drill-through navigation links
    - Add Employee 360 link per row navigating to `/employee/{employeeId}`
    - Add log-interaction link per row navigating to `/interaction` with `employeeId` query param
    - Use Angular Router for navigation
    - Ensure links are keyboard-accessible
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 7.2_

  - [x] 4.4 Wire filter and sort event handlers
    - Handle `filterChange` from StatusFilterComponent: update `activeFilter` signal, re-fetch
    - Handle `sortChange` from SortControlComponent: update `activeSort` signal, re-fetch
    - Include both params when both are active
    - Show loading indicator during re-fetch
    - _Requirements: 5.2, 5.3, 5.5, 6.2, 6.3, 6.5_

  - [x] 4.5 Wire FollowUpSection with filtered data
    - Compute follow-up entries from `entries` signal (filter where `followUpRequired === true`)
    - Pass filtered entries to FollowUpSectionComponent
    - _Requirements: 4.1_

  - [x] 4.6 Add responsive CSS and accessibility attributes
    - Implement responsive breakpoint at 768px (stacked cards or horizontal scroll)
    - Ensure WCAG AA colour contrast on status badges (use CSS custom properties from design)
    - Add `aria-live` region for loading announcements
    - _Requirements: 7.4, 7.5, 7.6_

- [x] 5. Integrate into DashboardComponent
  - [x] 5.1 Wire InteractionMatrixComponent into DashboardComponent
    - Modify `src/app/dashboard/dashboard.component.ts`: import `InteractionMatrixComponent`, add to `imports` array
    - Modify `src/app/dashboard/dashboard.component.html`: replace skeleton cards section with `<app-interaction-matrix>`
    - Remove `skeletonCards` property from DashboardComponent class
    - _Requirements: 2.1_

- [x] 6. Checkpoint - Verify integration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Write unit and property tests
  - [x] 7.1 Write unit tests for InteractionMatrixComponent
    - Create `src/app/dashboard/interaction-matrix/interaction-matrix.component.spec.ts`
    - Test rows rendered per entry count
    - Test loading indicator shown while fetching, hidden once data arrives
    - Test error message + retry button on HTTP error
    - Test correct CSS class per EngagementStatus value
    - Test drill-through links contain correct router paths with employee ID
    - Test follow-up section shows only `followUpRequired: true` entries
    - Test "No follow-ups needed" message when all entries have `followUpRequired: false`
    - Test filter selection triggers new API call with correct query parameter
    - Test semantic HTML structure (table, thead, tbody)
    - Test aria-busy/aria-live region present during loading
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9_

  - [x] 7.2 Write property test for follow-up filtering correctness
    - **Property 2: Follow-up filtering correctness**
    - Use fast-check to generate arrays of MatrixEntry with random `followUpRequired` booleans
    - Assert filtered result equals `entries.filter(e => e.followUpRequired)` — same items, same order
    - **Validates: Requirements 2.4, 4.1**

  - [x] 7.3 Write property test for status indicator mapping
    - **Property 3: Status indicator mapping**
    - Use fast-check with `fc.constantFrom('OVERDUE', 'AT_RISK', 'ON_TRACK')`
    - Assert mapping function returns correct CSS class AND aria-label for every status
    - **Validates: Requirements 2.3, 7.3, 8.5**

  - [x] 7.4 Write property test for drill-through link correctness
    - **Property 4: Drill-through link correctness**
    - Use fast-check with `fc.nat({ min: 1 })` for employeeId
    - Assert Employee 360 path is `/employee/{id}` and log-interaction path is `/interaction` with `employeeId={id}` query param
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 4.3**

  - [x] 7.5 Write property test for recency and date display formatting
    - **Property 5: Recency and date display formatting**
    - Use fast-check to generate optional recency (null or nat) and optional lastInteractionDate (null or ISO string)
    - Assert null recency → "No interactions", number → "{n} days"; null date → "Never", non-null → formatted date
    - **Validates: Requirements 2.2**

- [x] 8. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- All new components are standalone (no NgModules) following project conventions
- The backend API is already implemented — this plan is frontend-only

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "2.1", "2.2", "3.1"] },
    { "id": 2, "tasks": ["1.3", "1.4", "4.1"] },
    { "id": 3, "tasks": ["4.2", "4.3", "4.4", "4.5", "4.6"] },
    { "id": 4, "tasks": ["5.1"] },
    { "id": 5, "tasks": ["7.1", "7.2", "7.3", "7.4", "7.5"] }
  ]
}
```
