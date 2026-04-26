package com.example.demo.utils;

public class EloCalculator {

    private static final int[] FIDE_D_TABLE = {
            3, 10, 17, 24, 31, 38, 46, 53, 61, 68, 76, 83, 91, 98, 106, 113, 121, 129, 137, 145,
            153, 162, 170, 179, 188, 197, 206, 215, 225, 235, 245, 256, 267, 278, 290, 302, 315,
            328, 344, 357, 374, 391, 411, 432, 456, 484, 517, 559, 619, 735
    };

    /**
     * Calculates the expected score for player A against player B based on their ratings using the official FIDE table.
     * @param ratingA Recent monthly rating of player A
     * @param ratingB Recent monthly rating of player B
     * @return The expected score (win probability) between 0.0 and 1.0
     */
    public double calculateExpectedScore(int ratingA, int ratingB){
        int diff = ratingA - ratingB;
        boolean aIsHigher = diff >= 0;
        int d = Math.abs(diff);

        // FIDE 400-point rule (a difference of > 400 is treated as exactly 400)
        // The "Hikaru Rule" Amendment (2025): Removed for players rated 2650 or higher
        if (d > 400 && ratingA < 2650) {
            d = 400;
        }

        int pd = 50;
        int i = 0;
        while (i < FIDE_D_TABLE.length && d > FIDE_D_TABLE[i]) {
            pd++;
            i++;
        }

        return aIsHigher ? (double) pd / 100 : (double) (100 - pd) / 100;
    }

    /**
     * Calculates the rating change for a player based on k-factor, the actualScore, and the expectedScore
     * @param k K-factor
     * @param actualScore result of the game for a player (1.0 for a win, 0.5 for a draw, and 0.0 for a loss)
     * @param expectedScore win probability between 0.0 and 1.0
     * @return The rating change (rating gain/loss), rounded to one decimal place
     */
    public double calculateRatingChange(int k, double actualScore, double expectedScore) {
        double rawChange = k * (actualScore - expectedScore);
        return Math.round(rawChange * 10.0) / 10.0;
    }

    /**
     * Finds Time Control Type (blitz, rapid, or std)
     * @param time total time given in the beginning of the game
     * @return time control type
     */
    public TimeControl findTimeControlType(double time) {
        time = Math.abs(time);
        if(time <= 10.0) return TimeControl.BLITZ;
        else if( time < 45.0) return TimeControl.RAPID;
        return TimeControl.STD;
    }
}
