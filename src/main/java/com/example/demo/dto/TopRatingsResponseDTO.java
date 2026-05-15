package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TopRatingsResponseDTO {
    public List<TopRatingDTOResponse> stdRatings;
    public List<TopRatingDTOResponse> rapidRatings;
    public List<TopRatingDTOResponse> blitzRatings;
}
