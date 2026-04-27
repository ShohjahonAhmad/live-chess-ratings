package com.example.demo.dto;

import com.example.demo.entity.BroadcastRound;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopDTO {
    public List<BroadcastDTO> active;
    public static class BroadcastDTO {
        public static class TournamentDTO {
            public String id;
            public String name;
            public String slug;
            public long[] dates;
            public String description;
            public BroadcastInfoDTO info;
        }

        public TournamentDTO tour;
        public RoundDTO round;
        public Object group;


        public static class RoundDTO {
            public String id;
            public String name;
            public String slug;
            public boolean finished;
            public boolean ongoing;
            public Long startsAt;
            public Long finishedAt;
            public boolean rated;
        }

        public static class BroadcastInfoDTO {
            public String format;
            public String tc;
            public String location;
        }
    }
}