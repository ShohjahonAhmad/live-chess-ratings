package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TopRatingsResponseDTO {
    public List<TopRatingDTO> stdRatings;
    public List<TopRatingDTO> rapidRatings;
    public List<TopRatingDTO> blitzRatings;
}
