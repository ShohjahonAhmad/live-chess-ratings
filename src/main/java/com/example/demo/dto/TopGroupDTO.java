package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopGroupDTO {
    public static class GroupDTO{
        public static class TournamentDTO{
            public String id;
            public boolean live;
        }
        public List<TournamentDTO> tours;
    }
    public GroupDTO group;
}


