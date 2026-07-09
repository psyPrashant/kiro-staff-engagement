@story:KSE-47
Feature: User Login

  Scenario Outline: Successful login with valid credentials
    Given the user navigates to the login page
    When the user logs in with email "<email>" and password "<password>"
    Then the user should be redirected to the home page

    Examples:
      | email                  | password  |
      | admin@psybergate.co.za | Password1 |

  Scenario Outline: Failed login with invalid credentials
    Given the user navigates to the login page
    When the user logs in with email "<email>" and password "<password>"
    Then the user should see the error message "<error_message>"
    And the user should remain on the login page

    Examples:
      | email                  | password      | error_message             |
      | wrong@example.com      | Password1     | Invalid email or password |
      | admin@psybergate.co.za | wrongpassword | Invalid email or password |
