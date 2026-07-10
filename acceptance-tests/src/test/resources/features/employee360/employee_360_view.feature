@employee360
Feature: Employee 360 View

  Scenario: Authenticated user navigates to employee 360 and sees full profile
    Given the user navigates to the login page
    When the user logs in with email "admin@psybergate.co.za" and password "Password1"
    And the user navigates to the employee 360 view for employee 1
    Then the profile summary should be visible
    And the interaction history should be visible
    And the open tasks should be visible

  Scenario: Employee with no interactions shows empty-state messages
    Given the user navigates to the login page
    When the user logs in with email "admin@psybergate.co.za" and password "Password1"
    And the user navigates to the employee 360 view for employee 2
    Then the profile summary should be visible
    And the empty interactions message should be displayed
    And the empty tasks message should be displayed

  Scenario: Overdue tasks are visually distinguished from non-overdue tasks
    Given the user navigates to the login page
    When the user logs in with email "admin@psybergate.co.za" and password "Password1"
    And the user navigates to the employee 360 view for employee 1
    Then overdue tasks should be visually distinguished from non-overdue tasks

  Scenario: Unauthenticated user is redirected to login
    When the user navigates to the employee 360 view for employee 1 without logging in
    Then the user should be redirected to the login page
