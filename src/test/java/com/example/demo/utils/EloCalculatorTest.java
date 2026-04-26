package com.example.demo.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


public class EloCalculatorTest {

    private final EloCalculator eloCalculator = new EloCalculator();


    @Test
    public void testThatFindTimeControlTypeGivesCorrectEnum() {
        TimeControl result1 = eloCalculator.findTimeControlType(45);

        assertThat(result1).isEqualTo(TimeControl.STD);

        TimeControl result2 = eloCalculator.findTimeControlType(44.99);

        assertThat(result2).isEqualTo(TimeControl.RAPID);

        TimeControl result3 = eloCalculator.findTimeControlType(10);

        assertThat(result3).isEqualTo(TimeControl.BLITZ);

        TimeControl result4 = eloCalculator.findTimeControlType(-12);
        assertThat(result4).isEqualTo(TimeControl.RAPID);
    }

    @Test
    public void testThatCalculateExpectedScoreReturnsCorrectOutput() {
        double result1 = eloCalculator.calculateExpectedScore(2200, 2400);
        assertThat(result1).isEqualTo(0.24);

        double result2 = eloCalculator.calculateExpectedScore(2400, 2200);
        assertThat(result2).isEqualTo(0.76);
    }

    @Test
    public void testThatCalculateExpectedScoreIncludes400_pointRule() {
        double result1 = eloCalculator.calculateExpectedScore(2550, 2000);
        assertThat(result1).isEqualTo(0.92);

        double result2 = eloCalculator.calculateExpectedScore(2000, 2550);
        assertThat(result2).isEqualTo(0.08);
    }

    @Test
    public void testThatCalculateExpectedScoreExclude400_pointRuleTopPlayers() {
        // Top player (2700): 400-rule skipped, full 500 diff used
        double result1 = eloCalculator.calculateExpectedScore(2700, 2200);
        assertThat(result1).isEqualTo(0.96);

        // Lower player (2200): 400-rule applies, capped at 400
        double result2 = eloCalculator.calculateExpectedScore(2200, 2700);
        assertThat(result2).isEqualTo(0.08);

        // Key — they do NOT sum to 1.0 (asymmetry is intentional)
        assertThat(result1 + result2).isNotEqualTo(1.0);
    }

    @Test
    public void testThatCalculateRatingChangeReturnsCorrectOutput() {
        double result1 = eloCalculator.calculateRatingChange(10, 1.0, 0.76);

        assertThat(result1).isEqualTo(2.4);

        double result2 = eloCalculator.calculateRatingChange(20, 1.0, 0.24);

        assertThat(result2).isEqualTo(15.2);

        double result3 = eloCalculator.calculateRatingChange(10, 0.0, 0.76);

        assertThat(result3).isEqualTo(-7.6);

        double result4 = eloCalculator.calculateRatingChange(30, 0.5, 0.55);

        assertThat(result4).isEqualTo(-1.5);
    }
}
