package com.example.demo.dto;

import com.example.demo.utils.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentGamesDTO {
    public String id;
    public Long opponentFideId;
    public String opponentName;
    public Double change;
    public Result result;
    public String date;
    public String round;
    public String tournament;
    public String createdAt;
}
