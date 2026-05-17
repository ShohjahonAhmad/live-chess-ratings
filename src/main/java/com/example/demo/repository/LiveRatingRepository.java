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
                SELECT
                    p.fide_id, p.name, p.country, p.birthday,
                    l.std_rating AS "rating",
                    l.std_change AS "ratingChange",
                    COUNT(g.id) AS "count",
                    JSON_AGG(
                        JSON_BUILD_OBJECT(
                            'id',           g.id,
                            'opponentFideId', CASE WHEN p.fide_id = g.white_fide_id THEN bp.fide_id ELSE wp.fide_id END,
                            'opponentName',  CASE WHEN p.fide_id = g.white_fide_id THEN bp.name ELSE wp.name END,
                            'opponentRating', CASE WHEN p.fide_id = g.white_fide_id THEN g.black_rating ELSE g.white_rating END,
                            'change',        CASE WHEN p.fide_id = g.white_fide_id THEN g.white_rating_change ELSE g.black_rating_change END,
                            'result',        g.result,
                            'date',          g.date,
                            'round',         r.name,
                            'tournament',    t.name,
                            'createdAt',    g.created_at
                        ) ORDER BY g.created_at ASC
                    ) FILTER (WHERE g.id IS NOT NULL) AS "recentGames"
                FROM live_ratings l
                JOIN players p ON l.fide_id = p.fide_id
                LEFT JOIN games g ON g.time_control = 'STD'
                                  AND (g.black_fide_id = p.fide_id OR g.white_fide_id = p.fide_id)
                                  AND g.date >= NOW() - INTERVAL '1 month'
                LEFT JOIN rounds r ON g.round_id = r.id
                LEFT JOIN tournaments t ON r.tournament_id = t.id
                LEFT JOIN players wp ON g.white_fide_id = wp.fide_id
                LEFT JOIN players bp ON g.black_fide_id = bp.fide_id
                GROUP BY p.fide_id, p.name, p.country, p.birthday, l.std_rating, l.std_change
                HAVING p.flag = '' OR p.flag NOT IN ('i', 'wi') OR COUNT(g.id) > 0
                ORDER BY l.std_rating DESC
                LIMIT 300;
            """;

    String top300RapidQuery = """
                SELECT
                    p.fide_id, p.name, p.country, p.birthday,
                    l.rapid_rating AS "rating",
                    l.rapid_change AS "ratingChange",
                    COUNT(g.id) AS "count",
                    JSON_AGG(
                        JSON_BUILD_OBJECT(
                            'id',           g.id,
                            'opponentFideId', CASE WHEN p.fide_id = g.white_fide_id THEN bp.fide_id ELSE wp.fide_id END,
                            'opponentName',  CASE WHEN p.fide_id = g.white_fide_id THEN bp.name ELSE wp.name END,
                            'change',        CASE WHEN p.fide_id = g.white_fide_id THEN g.white_rating_change ELSE g.black_rating_change END,
                            'opponentRating', CASE WHEN p.fide_id = g.white_fide_id THEN g.black_rating ELSE g.white_rating END,
                            'result',        g.result,
                            'date',          g.date,
                            'round',         r.name,
                            'tournament',    t.name,
                            'createdAt',    g.created_at
                        ) ORDER BY g.created_at ASC
                    ) FILTER (WHERE g.id IS NOT NULL) AS "recentGames"
                FROM live_ratings l
                JOIN players p ON l.fide_id = p.fide_id
                LEFT JOIN games g ON g.time_control = 'RAPID'
                                  AND (g.black_fide_id = p.fide_id OR g.white_fide_id = p.fide_id)
                                  AND g.date >= NOW() - INTERVAL '1 month'
                LEFT JOIN rounds r ON g.round_id = r.id
                LEFT JOIN tournaments t ON r.tournament_id = t.id
                LEFT JOIN players wp ON g.white_fide_id = wp.fide_id
                LEFT JOIN players bp ON g.black_fide_id = bp.fide_id
                GROUP BY p.fide_id, p.name, p.country, p.birthday, l.rapid_rating, l.rapid_change
                HAVING p.rapid_flag = '' OR p.rapid_flag NOT IN ('i', 'wi') OR COUNT(g.id) > 0
                ORDER BY l.rapid_rating DESC
                LIMIT 300;
            """;

    String top300BlitzQuery = """
                SELECT
                    p.fide_id, p.name, p.country, p.birthday,
                    l.blitz_rating AS "rating",
                    l.blitz_change AS "ratingChange",
                    COUNT(g.id) AS "count",
                    JSON_AGG(
                        JSON_BUILD_OBJECT(
                            'id',           g.id,
                            'opponentFideId', CASE WHEN p.fide_id = g.white_fide_id THEN bp.fide_id ELSE wp.fide_id END,
                            'opponentName',  CASE WHEN p.fide_id = g.white_fide_id THEN bp.name ELSE wp.name END,
                            'opponentRating', CASE WHEN p.fide_id = g.white_fide_id THEN g.black_rating ELSE g.white_rating END,
                            'change',        CASE WHEN p.fide_id = g.white_fide_id THEN g.white_rating_change ELSE g.black_rating_change END,
                            'result',        g.result,
                            'date',          g.date,
                            'round',         r.name,
                            'tournament',    t.name,
                            'createdAt',    g.created_at
                        ) ORDER BY g.created_at ASC
                    ) FILTER (WHERE g.id IS NOT NULL) AS "recentGames"
                FROM live_ratings l
                JOIN players p ON l.fide_id = p.fide_id
                LEFT JOIN games g ON g.time_control = 'BLITZ'
                                  AND (g.black_fide_id = p.fide_id OR g.white_fide_id = p.fide_id)
                                  AND g.date >= NOW() - INTERVAL '1 month'
                LEFT JOIN rounds r ON g.round_id = r.id
                LEFT JOIN tournaments t ON r.tournament_id = t.id
                LEFT JOIN players wp ON g.white_fide_id = wp.fide_id
                LEFT JOIN players bp ON g.black_fide_id = bp.fide_id
                GROUP BY p.fide_id, p.name, p.country, p.birthday, l.blitz_rating, l.blitz_change
                HAVING p.blitz_flag = '' OR p.blitz_flag NOT IN ('i', 'wi') OR COUNT(g.id) > 0
                ORDER BY l.blitz_rating DESC
                LIMIT 300;
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

