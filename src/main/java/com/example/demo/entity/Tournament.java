package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    private String id;

    private String name;

    private String slug;

    private String format;

    private String tc;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BroadcastRound> rounds = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tournament that)) return false;
        return Objects.equals(id, that.getId()) &&
               Objects.equals(name, that.getName()) && 
               Objects.equals(slug, that.getSlug()) && 
               Objects.equals(format, that.getFormat()) && 
               Objects.equals(tc, that.getTc()) && 
               Objects.equals(location, that.getLocation()) && 
               Objects.equals(description, that.getDescription()) && 
               Objects.equals(startsAt, that.getStartsAt()) && 
               Objects.equals(endsAt, that.getEndsAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, slug, format, tc, location, description, startsAt, endsAt);
    }
}
