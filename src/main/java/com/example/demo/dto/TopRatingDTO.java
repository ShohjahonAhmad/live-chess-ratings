package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopRatingDTO {
    public Long rank;
    public Long fideId;
    public String name;
    public String country;
    public Short year;
    public String flag;
    public Double rating;
    public Double ratingChange;
    public Long count;
    public String recentGames;
}
