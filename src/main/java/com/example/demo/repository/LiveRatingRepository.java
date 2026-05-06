package com.example.demo.repository;

import com.example.demo.dto.TopRatingDTO;
import com.example.demo.entity.LiveRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveRatingRepository extends JpaRepository<LiveRating, Long> {
    String top300StdQuery = """
                SELECT p.fide_id, p.name, l.std_rating AS "rating", l.std_change AS "ratingChange", COUNT(g.id) AS "count" FROM live_ratings l
                JOIN players p
                    ON l.fide_id = p.fide_id
                LEFT JOIN games g
                    ON g.time_control = 'STD' AND (g.black_fide_id = p.fide_id OR g.white_fide_id = p.fide_id)
                GROUP by p.fide_id, p.name, l.std_rating, l.std_change
                HAVING p.flag = '' OR COUNT(g.id) > 0
                ORDER BY l.std_rating DESC LIMIT 300;
            """;

    String top300RapidQuery = """
                SELECT p.fide_id, p.name, l.rapid_rating AS "rating", l.rapid_change AS "ratingChange", COUNT(g.id) AS "count"
                FROM live_ratings l
                JOIN players p
                    ON l.fide_id = p.fide_id
                LEFT JOIN games g
                    ON g.time_control = 'RAPID' AND (g.black_fide_id = p.fide_id OR g.white_fide_id = p.fide_id)
                GROUP by p.fide_id, p.name, l.rapid_rating, l.rapid_change
                HAVING p.flag = '' OR COUNT(g.id) > 0
                ORDER BY l.rapid_rating DESC LIMIT 300;
            """;

    String top300BlitzQuery = """
                SELECT p.fide_id, p.name, l.blitz_rating AS "rating", l.blitz_change AS "ratingChange", COUNT(g.id) AS "count"
                FROM live_ratings l
                JOIN players p
                    ON l.fide_id = p.fide_id
                LEFT JOIN games g
                    ON g.time_control = 'BLITZ' AND (g.black_fide_id = p.fide_id OR g.white_fide_id = p.fide_id)
                GROUP by p.fide_id, p.name, l.blitz_rating, l.blitz_change
                HAVING p.flag = '' OR COUNT(g.id) > 0
                ORDER BY l.blitz_rating DESC LIMIT 300;
            """;

    LiveRating findByPlayerFideId(Long fideId);

    List<LiveRating> findAllByOrderByStdRatingDesc();

    @Query(value = top300StdQuery, nativeQuery = true)
    List<TopRatingDTO> findTop300StdPlayers();

    @Query(value = top300RapidQuery, nativeQuery = true)
    List<TopRatingDTO> findTop300RapidPLayers();

    @Query(value = top300BlitzQuery, nativeQuery = true)
    List<TopRatingDTO> findTop300BlitzPLayers();
}

