@scheduling
Feature: Interaction Scheduling

  Background:
    Given the user navigates to the login page
    And the user logs in with email "admin@psybergate.co.za" and password "Password1"

  Scenario: Manager schedules a next check-in from the interaction matrix
    Given the user is on the interaction matrix
    When the user clicks "Schedule Next" for employee "John Developer"
    And the user sets the scheduled date to a future date
    And the user selects interaction type "Check In"
    And the user submits the schedule form
    Then the calendar view should display an entry for "John Developer" with the scheduled date

  Scenario: Manager marks a scheduled interaction as completed
    Given the user has a pending scheduled interaction for "John Developer"
    And the user navigates to the schedule calendar
    When the user expands the entry for "John Developer"
    And the user clicks complete
    Then the entry for "John Developer" should no longer appear in the pending list

  Scenario: Overdue scheduled interaction is visually distinguished
    Given a past-dated pending scheduled interaction exists for "Jane Manager" via SQL seed
    And the user navigates to the schedule calendar
    Then the entry for "Jane Manager" should have an overdue indicator

  Scenario: Manager cannot schedule a check-in with a past date
    Given the user is on the schedule form for employee "John Developer"
    When the user sets the scheduled date to a past date
    Then a date validation error should be displayed
    And the submit button should be disabled
