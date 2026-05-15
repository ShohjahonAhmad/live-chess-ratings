package com.example.demo.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopRatingDTOResponse {
    private Long fideId;
    private String name;
    private String country;
    private Short year;
    private Double rating;
    private Double ratingChange;
    private Long count;
    private List<RecentGamesDTO> recentGames;

    public static TopRatingDTOResponse from (TopRatingDTO dto, ObjectMapper mapper) throws JsonProcessingException {
        TopRatingDTOResponse response = new TopRatingDTOResponse();
        response.setFideId(dto.getFideId());
        response.setName(dto.getName());
        response.setCountry(dto.getCountry());
        response.setYear(dto.getYear());
        response.setRating(dto.getRating());
        response.setRatingChange(dto.getRatingChange());
        response.setCount(dto.getCount());
        if(dto.getRecentGames() != null) {
            List<RecentGamesDTO> recentGames = mapper.readValue(dto.getRecentGames(), mapper.getTypeFactory().constructCollectionType(List.class, RecentGamesDTO.class));
            response.setRecentGames(recentGames);
        }
        return response;
    }
}
