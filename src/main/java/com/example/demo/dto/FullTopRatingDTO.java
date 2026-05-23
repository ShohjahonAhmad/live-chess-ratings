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
public class FullTopRatingDTO {
    private Long rank;
    private Long fideId;
    private String name;
    private String country;
    private Short year;
    private Double rating;
    private Double ratingChange;
    private Long count;
    private List<RecentGamesDTO> recentGames;

    public FullTopRatingDTO(FullTopRatingDTO dto) {
        this.rank = dto.getRank();
        this.fideId = dto.getFideId();
        this.name = dto.getName();
        this.country = dto.getCountry();
        this.year = dto.getYear();
        this.rating = dto.getRating();
        this.ratingChange = dto.getRatingChange();
        this.count = dto.getCount();
        this.recentGames = dto.getRecentGames();
    }

    public static FullTopRatingDTO from (TopRatingDTO dto, ObjectMapper mapper) throws JsonProcessingException {
        FullTopRatingDTO response = new FullTopRatingDTO();
        response.setRank(dto.getRank());
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
