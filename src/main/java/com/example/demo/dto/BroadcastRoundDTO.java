package com.example.demo.dto;

public class BroadcastRoundDTO {

    public static class RoundDTO {
        public boolean ongoing;
        public boolean finished;
        public Long finishedAt;
    }
    ///
    public RoundDTO round;
}
