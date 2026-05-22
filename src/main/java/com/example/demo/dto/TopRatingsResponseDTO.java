package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TopRatingsResponseDTO {
    public TopRatingResponseDTO stdRatings;
    public TopRatingResponseDTO rapidRatings;
    public TopRatingResponseDTO blitzRatings;
}
