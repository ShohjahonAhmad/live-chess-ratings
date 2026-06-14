package com.example.demo.dto;

import com.example.demo.entity.Player;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
public class PlayerRatingHistoryDTO {
    private Player player;
    private RatingHistoryDTO ratingHistory;
}
