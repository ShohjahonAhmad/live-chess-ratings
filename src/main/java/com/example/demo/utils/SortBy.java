package com.example.demo.utils;

public enum SortBy {
    RATING("rating"),
    RATING_CHANGE("ratingChange"),
    YEAR("year"),
    COUNT("count");

    private final String value;

    SortBy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
