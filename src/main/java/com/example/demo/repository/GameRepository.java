package com.example.demo.repository;

import com.example.demo.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, String> {
    @Query(value = "SELECT EXISTS(SELECT 1 FROM games WHERE white_fide_id = :whiteFideId AND black_fide_id = :blackFideId AND round_id = :roundId AND move_count = :moveCount AND last_move = :lastMove)", nativeQuery = true)
    boolean existsByWhiteFideIdAndBlackFideIdAndRoundIdAndMoveCountAndLastMove(
            @Param("whiteFideId") Long whiteFideId,
            @Param("blackFideId") Long blackFideId,
            @Param("roundId") String roundId,
            @Param("moveCount") Short moveCount,
            @Param("lastMove") String lastMove
    );
}
