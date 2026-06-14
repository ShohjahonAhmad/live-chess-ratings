package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    public Short peakRating;
    public LocalDate peakRatingDate;
    public Double rating;
    public Double ratingChange;
    public Long count;
    public String recentGames;
}
