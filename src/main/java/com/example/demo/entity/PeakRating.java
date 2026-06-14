package com.example.demo.entity;

import com.example.demo.utils.TimeControl;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "peak_ratings", uniqueConstraints = {@UniqueConstraint(columnNames = {"fide_id", "time_control"})})
public class PeakRating {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fide_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_control", nullable = false)
    private TimeControl timeControl;

    @Column(name = "rating", nullable = false)
    private Short rating;

    @Column(name = "rating_date", nullable = false)
    private LocalDate ratingDate;
}
