package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopRatingResponseDTO {
    private List<FullTopRatingDTO> content;
    private Integer totalCount;
}
