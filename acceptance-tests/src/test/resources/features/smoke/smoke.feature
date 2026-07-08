@pre-push
Feature: Smoke Test

  Scenario: Application is accessible
    Given the application is running
    When I open the home page
    Then the page loads successfully
