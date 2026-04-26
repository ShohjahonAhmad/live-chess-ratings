package com.example.demo.entity;

import com.example.demo.utils.Result;
import com.example.demo.utils.TimeControl;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "games")
public class Game {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "round_id")
    private BroadcastRound round;

    @Column(name = "white_fide_id")
    private Long whiteFideId;

    @Column(name = "black_fide_id")
    private Long blackFideId;

    @Column(name = "white_rating")
    private Short whiteRating;

    @Column(name = "black_rating")
    private Short blackRating;

    @Column(name = "white_rating_change")
    private Double whiteRatingChange;

    @Column(name = "black_rating_change")
    private Double blackRatingChange;

    @Enumerated(EnumType.STRING)
    private Result result;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_control", columnDefinition = "varchar(10) default 'STD'")
    private TimeControl timeControl = TimeControl.STD;

    private LocalDate date;
}
