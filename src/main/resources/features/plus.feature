# language: en
Feature: plus
  In order to avoid silly mistakes
  Cashiers must be able to calculate a fraction

  @important
  Scenario: Regular numbers
    Given I have entered 6 into the calculator
    And I have entered 7 into the calculator
    When I press plus
    Then the stored result should be 13

  Scenario: More numbers
    Given I have entered 98 into the calculator
    And I have entered 34 into the calculator
    When I press plus
    Then the stored result should be 132
