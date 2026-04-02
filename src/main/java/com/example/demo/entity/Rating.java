package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "ratings", uniqueConstraints = {@UniqueConstraint(columnNames = {"fide_id", "period"})})
public class Rating {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fide_id")
    private Player player;

    private LocalDate period;

    @Column(name = "std_rating")
    private Short stdRating;

    @Column(name = "rapid_rating")
    private Short rapidRating;

    @Column(name = "blitz_rating")
    private Short blitzRating;
}
