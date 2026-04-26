package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "live_ratings")
public class LiveRating {

    @Id
    private Long fideId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "fide_id")
    private Player player;

    @Column(name = "std_rating")
    private Double stdRating;

    @Column(name = "rapid_rating")
    private Double rapidRating;

    @Column(name = "blitz_rating")
    private Double blitzRating;

    // Tracks the total change since the last official FIDE period
    @Column(name = "std_change")
    private Double stdChange = 0.0;

    @Column(name = "rapid_change")
    private Double rapidChange = 0.0;

    @Column(name = "blitz_change")
    private Double blitzChange = 0.0;
}

