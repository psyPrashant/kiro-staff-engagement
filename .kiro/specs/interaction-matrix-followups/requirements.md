# Requirements Document

## Introduction

The Interaction Matrix & Follow-Up Logic feature introduces a read-only analytics endpoint that computes an employee-level engagement matrix. For each employee, the system calculates recency (days since last interaction), frequency (total interaction count), and an engagement status flag. Employees whose engagement drops below configurable thresholds are flagged as overdue or at-risk, enabling managers to identify who needs a follow-up interaction. This is the richest logic in the application, so comprehensive unit testing of edge cases and boundary conditions is a primary deliverable alongside the endpoint itself.

## Glossary

- **Engagement_Engine**: The backend service component responsible for computing the interaction matrix and deriving engagement statuses for employees.
- **Interaction_Matrix**: A data structure containing one entry per employee with recency, frequency, and engagement status fields.
- **Engagement_Status**: An enumeration representing an employee's current engagement level: OVERDUE, AT_RISK, or ON_TRACK.
- **Recency**: The number of calendar days between the reference date and an employee's most recent interaction (based on `occurredAt`).
- **Frequency**: The total count of interactions recorded for an employee.
- **Overdue_Threshold**: The number of days (default: 30) after which an employee with no recent interaction is classified as OVERDUE.
- **At_Risk_Threshold**: The number of days (default: 14) after which an employee with no recent interaction is classified as AT_RISK.
- **Reference_Date**: The date against which recency is calculated; defaults to the current system date when the request is processed.
- **Follow_Up_Flag**: A boolean indicator set to true for employees whose Engagement_Status is OVERDUE or AT_RISK.

## Requirements

### Requirement 1: Compute Interaction Matrix

**User Story:** As a manager, I want to retrieve an engagement matrix for all employees, so that I can see at a glance who has been engaged recently and who has not.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/engagement/matrix`, THE Engagement_Engine SHALL return an Interaction_Matrix containing one entry per employee in the system; IF no employees exist, THEN THE Engagement_Engine SHALL return an empty JSON array with HTTP status 200.
2. THE Engagement_Engine SHALL compute Recency as the number of whole calendar days (truncated toward zero) between the employee's most recent interaction `occurredAt` date and the Reference_Date, such that an interaction occurring on the Reference_Date yields a Recency of 0.
3. THE Engagement_Engine SHALL compute Frequency as the total count of all interactions associated with the employee, counting each interaction record individually regardless of date.
4. WHEN an employee has zero interactions, THE Engagement_Engine SHALL set Recency to null and Frequency to zero.
5. THE Engagement_Engine SHALL include the employee's id, name, and email in each matrix entry.
6. THE Engagement_Engine SHALL return the matrix entries as a JSON array with HTTP status 200.
7. IF the Engagement_Engine cannot retrieve data due to a database connectivity failure, THEN THE Engagement_Engine SHALL return HTTP status 500 with an error message indicating that the matrix could not be computed.

### Requirement 2: Determine Engagement Status

**User Story:** As a manager, I want each employee to have an engagement status, so that I can quickly identify who is overdue or at risk of disengagement.

#### Acceptance Criteria

1. WHEN an employee has zero interactions, THE Engagement_Engine SHALL assign Engagement_Status OVERDUE to that employee.
2. WHEN an employee's Recency is greater than or equal to the Overdue_Threshold, THE Engagement_Engine SHALL assign Engagement_Status OVERDUE to that employee.
3. WHEN an employee's Recency is greater than or equal to the At_Risk_Threshold AND strictly less than the Overdue_Threshold, THE Engagement_Engine SHALL assign Engagement_Status AT_RISK to that employee.
4. WHEN an employee's Recency is strictly less than the At_Risk_Threshold, THE Engagement_Engine SHALL assign Engagement_Status ON_TRACK to that employee.
5. THE Engagement_Engine SHALL enforce that the At_Risk_Threshold is strictly less than the Overdue_Threshold.
6. THE Engagement_Engine SHALL evaluate engagement status using only the Recency value and configured thresholds, producing identical results for the same inputs regardless of invocation time or external state.

### Requirement 3: Follow-Up Flagging

**User Story:** As a manager, I want employees who need follow-ups to be clearly flagged, so that I can prioritise my engagement activities.

#### Acceptance Criteria

1. WHEN an employee's Engagement_Status is OVERDUE, THE Engagement_Engine SHALL set the Follow_Up_Flag to true for that employee.
2. WHEN an employee's Engagement_Status is AT_RISK, THE Engagement_Engine SHALL set the Follow_Up_Flag to true for that employee.
3. WHEN an employee's Engagement_Status is ON_TRACK, THE Engagement_Engine SHALL set the Follow_Up_Flag to false for that employee.
4. THE Engagement_Engine SHALL include the Follow_Up_Flag as a boolean field in each Interaction_Matrix entry returned by the `/api/engagement/matrix` endpoint.

### Requirement 4: Configurable Thresholds

**User Story:** As an administrator, I want the overdue and at-risk thresholds to be configurable, so that the system can be tuned to different organisational engagement cadences.

#### Acceptance Criteria

1. THE Engagement_Engine SHALL read the Overdue_Threshold from the application configuration property `engagement.thresholds.overdue-days` with a default value of 30, accepting only integer values between 1 and 365 inclusive.
2. THE Engagement_Engine SHALL read the At_Risk_Threshold from the application configuration property `engagement.thresholds.at-risk-days` with a default value of 14, accepting only integer values between 1 and 365 inclusive.
3. IF the configured At_Risk_Threshold is greater than or equal to the configured Overdue_Threshold, THEN THE Engagement_Engine SHALL fail to start and produce an error message indicating that the at-risk threshold must be strictly less than the overdue threshold.
4. IF a configured threshold value is non-numeric, zero, negative, or exceeds 365, THEN THE Engagement_Engine SHALL fail to start and produce an error message indicating the invalid property name and the accepted range.

### Requirement 5: Matrix Sorting and Filtering

**User Story:** As a manager, I want to sort and filter the engagement matrix, so that I can focus on the employees who need attention most urgently.

#### Acceptance Criteria

1. WHEN the request includes a query parameter `status` with a valid Engagement_Status value (case-insensitive match against OVERDUE, AT_RISK, or ON_TRACK), THE Engagement_Engine SHALL return only employees whose computed Engagement_Status matches the requested value.
2. WHEN the request includes a query parameter `sort` with value `recency`, THE Engagement_Engine SHALL return entries sorted by Recency descending (longest gap first), with null-recency entries appearing first.
3. WHEN no `sort` parameter is provided, THE Engagement_Engine SHALL return entries sorted by employee name ascending using case-insensitive alphabetical ordering.
4. WHEN the request includes an invalid `status` parameter value (a value that does not match any Engagement_Status enumeration member after case-insensitive comparison), THE Engagement_Engine SHALL return HTTP 400 with an error message indicating the invalid value and listing the valid Engagement_Status options.
5. IF the request includes a `sort` parameter with a value other than `recency`, THEN THE Engagement_Engine SHALL return HTTP 400 with an error message indicating the unsupported sort value and listing the supported sort options.
6. WHEN both `status` and `sort` parameters are provided with valid values, THE Engagement_Engine SHALL first filter entries by the requested status, then apply the requested sort order to the filtered result set.

### Requirement 6: Last Interaction Date in Response

**User Story:** As a manager, I want to see the actual date of each employee's last interaction, so that I have full context alongside the computed days-since value.

#### Acceptance Criteria

1. THE Engagement_Engine SHALL include a `lastInteractionDate` field in each matrix entry containing the `occurredAt` date of the employee's most recent interaction, serialized as an ISO-8601 date string (yyyy-MM-dd).
2. WHEN an employee has zero interactions, THE Engagement_Engine SHALL set `lastInteractionDate` to null.
3. THE Engagement_Engine SHALL derive `lastInteractionDate` from the same most-recent interaction record used to compute the employee's Recency value.

### Requirement 7: Unit Testing of Engagement Logic

**User Story:** As a developer, I want comprehensive unit tests covering the engagement classification logic, so that edge cases are verified and regressions are caught early.

#### Acceptance Criteria

1. THE unit test suite SHALL verify that an employee with zero interactions receives Engagement_Status OVERDUE.
2. THE unit test suite SHALL verify that an employee whose Recency equals the Overdue_Threshold exactly (day 30 with default config) receives Engagement_Status OVERDUE.
3. THE unit test suite SHALL verify that an employee whose Recency equals the At_Risk_Threshold exactly (day 14 with default config) receives Engagement_Status AT_RISK.
4. THE unit test suite SHALL verify that an employee whose Recency is one day below the At_Risk_Threshold (day 13 with default config) receives Engagement_Status ON_TRACK.
5. THE unit test suite SHALL verify that an employee whose Recency is 0 (interaction occurred today) receives Engagement_Status ON_TRACK.
6. THE unit test suite SHALL verify that an employee whose Recency is one day above the Overdue_Threshold (day 31 with default config) receives Engagement_Status OVERDUE.
7. THE unit test suite SHALL verify that an employee whose Recency is one day above the At_Risk_Threshold but below the Overdue_Threshold (day 15 with default config) receives Engagement_Status AT_RISK.
8. THE unit test suite SHALL include a property-based test that generates random Recency values in range [0, 365] and random valid threshold configurations (At_Risk_Threshold in [1, 364], Overdue_Threshold in [At_Risk_Threshold+1, 365]) and verifies the Engagement_Engine produces a deterministic and consistent Engagement_Status for each combination.
9. THE unit tests SHALL use a fixed Reference_Date (injected via constructor or parameter) to ensure deterministic results independent of system clock.

### Requirement 8: Integration Testing with Real Database

**User Story:** As a developer, I want an integration test verifying the full endpoint against a real PostgreSQL database, so that I can confirm the query logic and response serialization work end-to-end.

#### Acceptance Criteria

1. THE integration test SHALL use Testcontainers to provision a PostgreSQL database with the same Flyway migrations as the production schema.
2. THE integration test SHALL seed at least three employees with varying interaction histories: one with no interactions, one with a recent interaction (within At_Risk_Threshold), and one with an old interaction (beyond Overdue_Threshold).
3. THE integration test SHALL invoke `GET /api/engagement/matrix` and verify the response contains correct Recency, Frequency, Engagement_Status, Follow_Up_Flag, and lastInteractionDate values for each seeded employee.
4. THE integration test SHALL verify that filtering by `status=OVERDUE` returns only employees classified as OVERDUE and excludes employees classified as AT_RISK or ON_TRACK.
5. THE integration test SHALL verify that the response returns HTTP 200 and a valid JSON array structure.
