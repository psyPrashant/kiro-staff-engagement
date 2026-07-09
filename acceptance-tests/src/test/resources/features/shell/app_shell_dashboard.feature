@story:KSE-APP-SHELL
Feature: App Shell and Dashboard

  Scenario: Authenticated user sees dashboard within the app shell
    Given the user navigates to the login page
    When the user logs in with email "admin@psybergate.co.za" and password "Password1"
    Then the user should see the dashboard
    And the navigation bar should be visible with links to all module areas
