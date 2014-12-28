Feature: minus
  In order to avoid silly mistakes
  Cashiers must be able to calculate a fraction

  @important
  Scenario: minus regular numbers
    Given I have entered 7 into the calculator
    And I have entered 2 into the calculator
    When I press minus
    Then the stored result should be 5

  Scenario: minus More numbers
    Given I have entered 9 into the calculator
    And I have entered 3 into the calculator
    When I press minus
    Then the stored result should be 6

