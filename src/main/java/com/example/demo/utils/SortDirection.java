package com.example.demo.utils;

public enum SortDirection {
    ASC("asc"),
    DESC("desc");


    private final String value;

    SortDirection(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
