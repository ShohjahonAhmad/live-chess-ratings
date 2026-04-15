package com.example.demo.entity;

import com.example.demo.utils.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Column(name = "ends_at")
    private Instant endsAt;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Game> games = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if (!(o instanceof BroadcastRound round)) return false;
        return Objects.equals(id, round.getId()) &&
               Objects.equals(name, round.getName()) &&
               Objects.equals(slug, round.getSlug()) &&
               status == round.getStatus() &&
               Objects.equals(startsAt, round.getStartsAt()) &&
               Objects.equals(endsAt, round.getEndsAt()) &&
               Objects.equals(tournament, round.getTournament());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, slug, status, startsAt, endsAt, tournament);
    }
}
