package com.example.demo.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static com.example.demo.utils.TimeControlParser.getLargestTimeInMinutes;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TimeControlParserTest {

    @Test
    void testThatGetLargestTimeInMinutesReturnsLargestMinute() {
        assertThat(getLargestTimeInMinutes("40/7200:0+10")).isEqualTo(120);
    }

    @Test
    void testThatGetLargestTimeInMinutesReturnsLargestMinuteFromSeconds() {
        assertThat(getLargestTimeInMinutes("900+5")).isEqualTo(15);

        assertThat(getLargestTimeInMinutes("5+900")).isEqualTo(15);

        assertThat(getLargestTimeInMinutes("300+5")).isEqualTo(5);

        assertThat(getLargestTimeInMinutes("5+300")).isEqualTo(5);

        assertThat(getLargestTimeInMinutes("5+ 3600")).isEqualTo(60);
    }

    @Test
    void testThatGetLargestTimeInMinutesReturnsLargestMinuteFromMinutes(){
        assertThat(getLargestTimeInMinutes("90+5")).isEqualTo(90);

        assertThat(getLargestTimeInMinutes("5+90")).isEqualTo(90);

        assertThat(getLargestTimeInMinutes("15+10")).isEqualTo(15);

        assertThat(getLargestTimeInMinutes("10+15")).isEqualTo(15);
    }

    @Test
    void testThatGetLargestTimeInMinutesHandlesEmptyString(){
        assertThat(getLargestTimeInMinutes("")).isEqualTo(0);
    }

    @Test
    void testThatGetLargestTimeInMinutesHandlesNull(){
        assertThatThrownBy(() -> getLargestTimeInMinutes(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testThatGetLargestTimeInMinutesHandlesSingleNumber() {
        assertThat(getLargestTimeInMinutes("3600")).isEqualTo(60);
    }
}
