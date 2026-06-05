package com.example.demo.utils;

public class TimeControlParser {

    public static int getLargestTimeInMinutes(String s) {
        int max = 0;
        int num = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                num = num * 10 + Character.getNumericValue(c);
            } else {
                max = Math.max(max, num);
                num = 0;
            }
        }

        max = Math.max(max, num);


        return max > 120 ? max / 60 : max;
    }
}
