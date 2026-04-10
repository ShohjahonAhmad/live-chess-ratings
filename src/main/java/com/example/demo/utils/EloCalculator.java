package com.example.demo.utils;

public class EloCalculator {


    /**
     * Calculates the expected score for player A against player B based on their ratings.
     * @param ratingA Recent monthly rating of player A
     * @param ratingB Recent monthly rating of player B
     * @return The expected score (win probability) between 0.0 and 1.0
     */
    public double calculateExpectedScore(int ratingA, int ratingB){
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }

    /**
     * Calculates the rating change for a player based on k-factor, the actualScore, and the expectedScore
     * @param k K-factor
     * @param actualScore result of the game for a player (1.0 for a win, 0.5 for a draw, and 0.0 for a loss)
     * @param expectedScore win probability between 0.0 and 1.0
     * @return The rating change (rating gain/loss)
     */
    public double calculateRatingChange(int k, double actualScore, double expectedScore) {
        return k * (actualScore - expectedScore);
    }
}
