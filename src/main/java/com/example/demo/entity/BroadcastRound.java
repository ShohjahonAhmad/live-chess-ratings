package com.example.demo.entity;

import com.example.demo.utils.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "rounds")
public class BroadcastRound {

    @Id
    private String id;

    private String name;

    private String slug;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "starts_at")
    private Instant startsAt;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;
}
