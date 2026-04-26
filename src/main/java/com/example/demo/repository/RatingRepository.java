package com.example.demo.repository;

import com.example.demo.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findTopByPlayerFideIdOrderByPeriodDesc(Long fideId);

    int countByPeriod(LocalDate date);
    List<Rating> findByPeriod(LocalDate date);
}
