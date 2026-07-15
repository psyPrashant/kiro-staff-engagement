@seed-data @backend-seed
Feature: Seed data is accessible via API

  As a developer I want automated confidence that the seed-data loader has populated
  the local environment and that records are accessible through the REST API.

  Background:
    Given the application is running with seed data loaded

  Scenario: Employees endpoint returns seeded records
    When I request all employees
    Then the response status is 200
    And the response contains at least 5 records

  Scenario: Companies endpoint returns seeded records
    When I request all companies
    Then the response status is 200
    And the response contains at least 2 records

  Scenario: Projects endpoint returns seeded records
    When I request all projects
    Then the response status is 200
    And the response contains at least 3 records

  Scenario: Users endpoint returns seeded records
    When I request all users
    Then the response status is 200
    And the response contains at least 3 records

  Scenario: Interactions endpoint returns seeded records with distinct types
    When I request all interactions
    Then the response status is 200
    And the response contains at least 3 records
    And the response contains at least 2 distinct interaction types

  Scenario: Tasks endpoint returns seeded records with expected statuses
    When I request all tasks
    Then the response status is 200
    And the response contains at least 3 records
    And the response contains at least one task with status "OPEN"
    And the response contains at least one task with status "DONE"
