@next-scheduled @backend-seed
Feature: Next Scheduled Interaction in API responses

  As a frontend developer,
  I want the Employee 360 and Employees List API responses to include the next scheduled interaction,
  so that the UI can display "Next check-in" information.

  Background:
    Given the application is running with seed data loaded

  Scenario Outline: Employee 360 returns correct nextScheduled for each interaction type
    Given a manager has scheduled a "<type>" interaction for employee 1 on a date 10 days from now
    When the client requests the employee 360 for employee 1
    Then the response status code is 200
    And the 360 response contains nextScheduled with scheduledAt 10 days from now and type "<type>"

    Examples:
      | type      |
      | CHECK_IN  |
      | MENTORING |
      | CATCH_UP  |
      | OTHER     |

  Scenario: Employee with no pending interactions has null nextScheduled
    Given employee 2 has no pending scheduled interactions
    When the client requests the employee 360 for employee 2
    Then the response status code is 200
    And the 360 response contains nextScheduled as null

  Scenario: Employee with only past-dated pending interactions has null nextScheduled
    Given a manager has scheduled a "CHECK_IN" interaction for employee 1 on a date 5 days ago
    And employee 1 has no future pending scheduled interactions
    When the client requests the employee 360 for employee 1
    Then the response status code is 200
    And the 360 response contains nextScheduled as null

  Scenario: Employees list returns correct nextScheduled per employee
    Given a manager has scheduled a "CHECK_IN" interaction for employee 1 on a date 7 days from now
    And employee 2 has no pending scheduled interactions
    When the client requests the employees list
    Then the response status code is 200
    And the employees list shows employee 1 with nextScheduled scheduledAt 7 days from now and type "CHECK_IN"
    And the employees list shows employee 2 with nextScheduled as null

  Scenario: Scheduling a nearer future interaction updates nextScheduled
    Given a manager has scheduled a "MENTORING" interaction for employee 1 on a date 20 days from now
    And the client requests the employee 360 for employee 1
    And the 360 response contains nextScheduled with scheduledAt 20 days from now and type "MENTORING"
    When a manager schedules a "CHECK_IN" interaction for employee 1 on a date 5 days from now
    And the client requests the employee 360 for employee 1
    Then the 360 response contains nextScheduled with scheduledAt 5 days from now and type "CHECK_IN"
