package com.example.demo.repository;

import com.example.demo.dto.TopRatingDTO;
import com.example.demo.entity.LiveRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveRatingRepository extends JpaRepository<LiveRating, Long> {
    String topStdQuery = """
                WITH player_games AS (
                    SELECT
                        white_fide_id AS fide_id,
                        id,
                        date,
                        result,
                        round_id,
                        created_at,
                        white_fide_id,
                        black_fide_id,
                        white_rating,
                        black_rating,
                        white_rating_change,
                        black_rating_change,
                        unknown_player_name
                    FROM games
                    WHERE time_control = 'STD'
            
                    UNION ALL
            
                    SELECT
                        black_fide_id AS fide_id,
                        id,
                        date,
                        result,
                        round_id,
                        created_at,
                        white_fide_id,
                        black_fide_id,
                        white_rating,
                        black_rating,
                        white_rating_change,
                        black_rating_change,
                        unknown_player_name
                    FROM games
                    WHERE time_control = 'STD'
                )
                SELECT
                    ROW_NUMBER() OVER (ORDER BY l.std_rating DESC) AS "rank",
                    p.fide_id, p.name, p.country, p.birthday, p.flag,
                    l.std_rating AS "rating",
                    l.std_change AS "ratingChange",
                    COUNT(g.id) AS "count",
                    JSON_AGG(
                        JSON_BUILD_OBJECT(
                            'id',           g.id,
                            'opponentFideId', CASE WHEN p.fide_id = g.white_fide_id THEN bp.fide_id ELSE wp.fide_id END,
                            'opponentName',  CASE WHEN p.fide_id = g.white_fide_id THEN COALESCE(bp.name, g.unknown_player_name) ELSE COALESCE(wp.name, g.unknown_player_name) END,
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
                LEFT JOIN player_games g
                    ON g.fide_id = p.fide_id
                LEFT JOIN rounds r ON g.round_id = r.id
                LEFT JOIN tournaments t ON r.tournament_id = t.id
                LEFT JOIN players wp ON g.white_fide_id = wp.fide_id
                LEFT JOIN players bp ON g.black_fide_id = bp.fide_id
                GROUP BY p.fide_id, p.name, p.country, p.birthday, l.std_rating, l.std_change
                ORDER BY l.std_rating DESC;
            """;

    String topRapidQuery = """
                WITH player_games AS (
                    SELECT
                        white_fide_id AS fide_id,
                        id,
                        date,
                        result,
                        round_id,
                        created_at,
                        white_fide_id,
                        black_fide_id,
                        white_rating,
                        black_rating,
                        white_rating_change,
                        black_rating_change,
                        unknown_player_name
                    FROM games
                    WHERE time_control = 'RAPID'
            
                    UNION ALL
            
                    SELECT
                        black_fide_id AS fide_id,
                        id,
                        date,
                        result,
                        round_id,
                        created_at,
                        white_fide_id,
                        black_fide_id,
                        white_rating,
                        black_rating,
                        white_rating_change,
                        black_rating_change,
                        unknown_player_name
                    FROM games
                    WHERE time_control = 'RAPID'
                )
                SELECT
                    ROW_NUMBER() OVER (ORDER BY l.rapid_rating DESC) AS "rank",
                    p.fide_id, p.name, p.country, p.birthday, p.flag,
                    l.rapid_rating AS "rating",
                    l.rapid_change AS "ratingChange",
                    COUNT(g.id) AS "count",
                    JSON_AGG(
                        JSON_BUILD_OBJECT(
                            'id',           g.id,
                            'opponentFideId', CASE WHEN p.fide_id = g.white_fide_id THEN bp.fide_id ELSE wp.fide_id END,
                            'opponentName',  CASE WHEN p.fide_id = g.white_fide_id THEN COALESCE(bp.name, g.unknown_player_name) ELSE COALESCE(wp.name, g.unknown_player_name) END,
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
                LEFT JOIN player_games g
                    ON g.fide_id = p.fide_id
                LEFT JOIN rounds r ON g.round_id = r.id
                LEFT JOIN tournaments t ON r.tournament_id = t.id
                LEFT JOIN players wp ON g.white_fide_id = wp.fide_id
                LEFT JOIN players bp ON g.black_fide_id = bp.fide_id
                GROUP BY p.fide_id, p.name, p.country, p.birthday, l.rapid_rating, l.rapid_change
                ORDER BY l.rapid_rating DESC;
            """;

    String topBlitzQuery = """
                WITH player_games AS (
                    SELECT
                        white_fide_id AS fide_id,
                        id,
                        date,
                        result,
                        round_id,
                        created_at,
                        white_fide_id,
                        black_fide_id,
                        white_rating,
                        black_rating,
                        white_rating_change,
                        black_rating_change,
                        unknown_player_name
                    FROM games
                    WHERE time_control = 'BLITZ'
            
                    UNION ALL
            
                    SELECT
                        black_fide_id AS fide_id,
                        id,
                        date,
                        result,
                        round_id,
                        created_at,
                        white_fide_id,
                        black_fide_id,
                        white_rating,
                        black_rating,
                        white_rating_change,
                        black_rating_change,
                        unknown_player_name
                    FROM games
                    WHERE time_control = 'BLITZ'
                )
                SELECT
                    ROW_NUMBER() OVER (ORDER BY l.blitz_rating DESC) AS "rank",
                    p.fide_id, p.name, p.country, p.birthday, p.flag,
                    l.blitz_rating AS "rating",
                    l.blitz_change AS "ratingChange",
                    COUNT(g.id) AS "count",
                    JSON_AGG(
                        JSON_BUILD_OBJECT(
                            'id',           g.id,
                            'opponentFideId', CASE WHEN p.fide_id = g.white_fide_id THEN bp.fide_id ELSE wp.fide_id END,
                            'opponentName',  CASE WHEN p.fide_id = g.white_fide_id THEN COALESCE(bp.name, g.unknown_player_name) ELSE COALESCE(wp.name, g.unknown_player_name) END,
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
                LEFT JOIN player_games g
                    ON g.fide_id = p.fide_id
                LEFT JOIN rounds r ON g.round_id = r.id
                LEFT JOIN tournaments t ON r.tournament_id = t.id
                LEFT JOIN players wp ON g.white_fide_id = wp.fide_id
                LEFT JOIN players bp ON g.black_fide_id = bp.fide_id
                GROUP BY p.fide_id, p.name, p.country, p.birthday, l.blitz_rating, l.blitz_change
                HAVING p.blitz_flag = '' OR p.blitz_flag NOT IN ('i', 'wi') OR COUNT(g.id) > 0
                ORDER BY l.blitz_rating DESC;
            """;

    LiveRating findByPlayerFideId(Long fideId);

    List<LiveRating> findAllByOrderByStdRatingDesc();

    @Query(
        value = topStdQuery,
        nativeQuery = true
    )
    List<TopRatingDTO> findStdPlayers();


    @Query(
        value = topRapidQuery,
        nativeQuery = true
    )
    List<TopRatingDTO> findRapidPLayers();


    @Query(value = topBlitzQuery, nativeQuery = true)
    List<TopRatingDTO> findBlitzPLayers();

}

