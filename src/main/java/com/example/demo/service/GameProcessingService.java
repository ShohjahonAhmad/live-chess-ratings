package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.LiveRatingRepository;
import com.example.demo.repository.PlayerRepository;
import com.example.demo.repository.RatingRepository;
import com.example.demo.utils.EloCalculator;
import com.example.demo.utils.Result;
import com.example.demo.utils.TimeControl;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.demo.utils.TimeControlParser.getLargestTimeInMinutes;

@Service
public class GameProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(GameProcessingService.class);
    private static final Pattern ID_PATTERN = Pattern.compile("\\[(?:Site|GameURL) \".*/([^/\"{}]+)\"]");
    private static final Pattern WHITE_FIDE_PATTERN = Pattern.compile("\\[WhiteFideId \"(\\d+)\"]");
    private static final Pattern BLACK_FIDE_PATTERN = Pattern.compile("\\[BlackFideId \"(\\d+)\"]");
    private static final Pattern WHITE_NAME_PATTERN = Pattern.compile("\\[White \"([^\"]+)\"]");
    private static final Pattern BLACK_NAME_PATTERN = Pattern.compile("\\[Black \"([^\"]+)\"]");
    private static final Pattern RESULT_PATTERN = Pattern.compile("\\[Result \"([^\"]+)\"]");
    private static final Pattern WHITE_RATING_PATTERN = Pattern.compile("\\[WhiteElo \"(\\d+)\"]");
    private static final Pattern BLACK_RATING_PATTERN = Pattern.compile("\\[BlackElo \"(\\d+)\"]");
    private static final Pattern TIME_CONTROL_PATTERN = Pattern.compile("\\[TimeControl \"([^\"]+)\"]");
    private static final Pattern MOVE_PATTERN = Pattern.compile("\\d+\\.{1,3}\\s+(\\S+)(?:\\s+\\{[^}]*})?");
    Pattern MINUTE_PATTERN = Pattern.compile("(\\d+)\\s*(min|minutes)", Pattern.CASE_INSENSITIVE);;

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final RatingRepository ratingRepository;
    private final LiveRatingRepository liveRatingRepository;


    public GameProcessingService(GameRepository gameRepository, PlayerRepository playerRepository, RatingRepository ratingRepository, LiveRatingRepository liveRatingRepository) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.ratingRepository = ratingRepository;
        this.liveRatingRepository = liveRatingRepository;
    }

    @Transactional
    public void processPgn(String pgn, BroadcastRound round, Map<String, Boolean> ignoredGames) {
        Tournament tournament = round.getTournament();

        Game game;
        try {
            game = getGame(pgn, ignoredGames, tournament, round);
        } catch (RuntimeException e) {
            return;
        }
        game.setRound(round);

        Player whitePlayer = getPlayer(game.getWhiteFideId());
        Player blackPlayer = getPlayer(game.getBlackFideId());

        TimeControl white = null;
        TimeControl black = null;

        if(game.getWhiteRating() != null && whitePlayer != null) {
            Rating rating = ratingRepository.findById(whitePlayer.getFideId()).orElse(null);
            white = resolveTimeControlBasedOnRatings(game, rating);
        }

        if(game.getBlackRating() != null && blackPlayer != null){
            Rating rating = ratingRepository.findById(blackPlayer.getFideId()).orElse(null);
            black = resolveTimeControlBasedOnRatings(game, rating);
        }

        if(white != null && black == null){
            game.setTimeControl(white);
        } else if(white == null && black != null){
            game.setTimeControl(black);
        } else if(white == black) {
            game.setTimeControl(white);
        }

        if(game.getTimeControl() == null){
            String timeControl = extractMatch(pgn, TIME_CONTROL_PATTERN, 1);
            game.setTimeControl(resolveTimeControl(timeControl, tournament));
        }

        setGameUnknownPlayerName(pgn, game, whitePlayer, blackPlayer);

        if(areBothPlayersNull(whitePlayer, blackPlayer)) {
            ignoredGames.put(game.getId(), true);
            return;
        }

        // Set the rating changes on the game object
        updateGameRatingChange(whitePlayer, game);
        updateGameRatingChange(blackPlayer, game);

        // Update live ratings
        updateLiveRatingByTimeControl(whitePlayer, game.getTimeControl(), game.getWhiteRatingChange());
        updateLiveRatingByTimeControl(blackPlayer, game.getTimeControl(), game.getBlackRatingChange());

        gameRepository.save(game);
        logger.debug("Game {} saved: {} vs {} - Result: {}", game.getId(), game.getWhiteFideId(), game.getBlackFideId(), game.getResult());
    }

    private TimeControl resolveTimeControlBasedOnRatings(Game game, Rating rating) {
        if(rating != null) {
            if(Objects.equals(rating.getStdRating(), game.getWhiteRating()) && !Objects.equals(rating.getBlitzRating(), game.getWhiteRating()) && !Objects.equals(rating.getRapidRating(), game.getWhiteRating())){
                return TimeControl.STD;
            } else if(Objects.equals(rating.getRapidRating(), game.getWhiteRating()) && !Objects.equals(rating.getBlitzRating(), game.getWhiteRating()) && !Objects.equals(rating.getStdRating(), game.getWhiteRating())){
                return TimeControl.RAPID;
            } else if(Objects.equals(rating.getBlitzRating(), game.getWhiteRating()) && !Objects.equals(rating.getRapidRating(), game.getWhiteRating()) && !Objects.equals(rating.getStdRating(), game.getWhiteRating())){
                return TimeControl.BLITZ;
            }
        }

        return null;
    }

    private void setGameUnknownPlayerName(String pgn, Game game, Player whitePlayer, Player blackPlayer) {
        if(whitePlayer == null) game.setUnknownPlayerName(extractMatch(pgn, WHITE_NAME_PATTERN, 1));
        if(blackPlayer == null) game.setUnknownPlayerName(extractMatch(pgn, BLACK_NAME_PATTERN, 1));
    }

    private Game getGame(String pgn, Map<String, Boolean> ignoredGames, Tournament tournament, BroadcastRound round ) {
        String gameId = extractMatch(pgn, ID_PATTERN, 1);
        String result = extractMatch(pgn, RESULT_PATTERN, 1);
        if(isGameGoing(result)) throw new IllegalStateException("Game is still going");

        Matcher m = MOVE_PATTERN.matcher(pgn);
        String lastMove = "";
        short count = 0;
        while(m.find()){
            lastMove = m.group(1);
            count++;
        }

        if(count == 0) {
            logger.debug("Game {} has no moves yet, skipping until moves arrive", gameId);
            throw new IllegalArgumentException("Game has no moves yet: " + gameId);
        }

        Game game = new Game();
        if(gameId == null || ignoredGames.containsKey(gameId)) throw new IllegalArgumentException("Game already processed or ignored: " + gameId);
        if(gameRepository.existsById(gameId)) {
            ignoredGames.put(gameId, true);
            throw new IllegalArgumentException("Game with id " + gameId + " already exists in database");
        }
        game.setId(gameId);
        game.setDate(LocalDate.now());

        String whiteFideId = extractMatch(pgn, WHITE_FIDE_PATTERN, 1);
        game.setWhiteFideId(whiteFideId == null ? null : Long.parseLong(whiteFideId));

        String blackFideId = extractMatch(pgn, BLACK_FIDE_PATTERN, 1);
        game.setBlackFideId(blackFideId == null ? null : Long.parseLong(blackFideId));

        Result resultEnum = "1-0".equals(result) ? Result.WIN : "0-1".equals(result) ? Result.LOSS : Result.DRAW;
        game.setResult(resultEnum);

        logger.debug("Extracted last move: {} and move count: {} for game {}", lastMove, count, gameId);

        game.setLastMove(lastMove);
        game.setMoveCount(count);

        if(gameRepository.existsByWhiteFideIdAndBlackFideIdAndRoundIdAndMoveCountAndLastMove(game.getWhiteFideId(), game.getBlackFideId(), round.getId(), game.getMoveCount(), game.getLastMove())) {
            ignoredGames.put(gameId, true);
            throw new IllegalArgumentException("Game with same players, round, result, move count and last move already exists");
        }

        String whiteRatingPgn = extractMatch(pgn, WHITE_RATING_PATTERN, 1);
        short whiteRating = Short.parseShort(whiteRatingPgn != null ? whiteRatingPgn : "0");
        game.setWhiteRating(whiteRating);

        String blackRatingPgn = extractMatch(pgn, BLACK_RATING_PATTERN, 1);
        short blackRating = Short.parseShort(blackRatingPgn != null ? blackRatingPgn : "0");
        game.setBlackRating(blackRating);

        return game;
    }

    private boolean isGameGoing(String result) {
        return result == null || result.contains("*");
    }

    private TimeControl resolveTimeControl (String timeControl, Tournament tournament) {
        TimeControl tcType = TimeControl.STD;

        if (timeControl != null) {
            try {
                int timeValue = getLargestTimeInMinutes(timeControl.trim());
                tcType = EloCalculator.findTimeControlType(timeValue);
            } catch (Exception e) {
                // Ignore parse errors safely
            }
        } else {
            // If time control is missing, try to infer from tournament settings
            if (tournament.getTc() != null) {
                try {
                    Matcher matcher = MINUTE_PATTERN.matcher(tournament.getTc());
                    double maxMinutes = 0;
                    while(matcher.find()){
                        double timeValue = Double.parseDouble(matcher.group(1));
                        maxMinutes = Math.max(timeValue, maxMinutes);
                    }
                    if(maxMinutes > 0) {
                        tcType = EloCalculator.findTimeControlType(maxMinutes);
                    }
                } catch (Exception e) {
                    // fallback to std if parsing fails
                }
            }
        }
        return tcType;
    }

    private Player getPlayer(Long fideId){
        if(fideId == null) return null;

        return playerRepository.findById(fideId).orElse(null);
    }

    private boolean areBothPlayersNull(Player whitePlayer, Player blackPlayer) {
        return whitePlayer == null && blackPlayer == null;
    }

    private void updateGameRatingChange(Player player, Game game) {
        if(player == null || game.getTimeControl() == null) return;
        int k = getKFactor(player, game.getTimeControl());

        boolean isWhitePlayer = Objects.equals(player.getFideId(), game.getWhiteFideId());

        double ratingChange = getRatingChange(game, isWhitePlayer, k);

        if(isWhitePlayer) {
            game.setWhiteRatingChange(ratingChange);
        } else{
            game.setBlackRatingChange(ratingChange);
        }
    }

    private int getKFactor(Player player, TimeControl timeControl) {
        return switch (timeControl) {
            case TimeControl.BLITZ -> player.getBlitzK() != null ? player.getBlitzK() : 20;
            case TimeControl.RAPID -> player.getRapidK() != null ? player.getRapidK() : 20;
            case TimeControl.STD   -> player.getStdK() != null ? player.getStdK() : 20;
        };
    }

    private double getRatingChange(Game game, boolean isWhitePlayer, int k) {
        double actualScore = getActualScore(isWhitePlayer, game.getResult());
        double expectedScore = getExpectedScore(isWhitePlayer, game.getWhiteRating(), game.getBlackRating());

        return EloCalculator.calculateRatingChange(k, actualScore, expectedScore);
    }

    private double getActualScore(boolean isWhitePlayer, Result result) {
        if (isWhitePlayer) {
            return result == Result.WIN ? 1.0 : (result == Result.LOSS ? 0.0 : 0.5);
        }
        return result == Result.LOSS ? 1.0 : (result == Result.WIN ? 0.0 : 0.5);
    }

    private double getExpectedScore(boolean isWhitePlayer, short whiteRating, short blackRating) {
        if (isWhitePlayer) {
            return EloCalculator.calculateExpectedScore(whiteRating, blackRating);
        }
        return EloCalculator.calculateExpectedScore(blackRating, whiteRating);
    }

    private void updateLiveRatingByTimeControl(Player player, TimeControl timeControl, Double ratingChange){
        if(player == null || ratingChange == null) return;
        LiveRating liveRating = liveRatingRepository.findById(player.getFideId()).orElse(null);
        if (liveRating == null) return;

        switch (timeControl) {
            case TimeControl.BLITZ -> {
                liveRating.setBlitzRating(addAndRound(liveRating.getBlitzRating(), ratingChange));
                liveRating.setBlitzChange(addAndRound(liveRating.getBlitzChange(), ratingChange));
            }
            case TimeControl.RAPID -> {
                liveRating.setRapidRating(addAndRound(liveRating.getRapidRating(), ratingChange));
                liveRating.setRapidChange(addAndRound(liveRating.getRapidChange(), ratingChange));
            }
            case TimeControl.STD   -> {
                liveRating.setStdRating(addAndRound(liveRating.getStdRating(), ratingChange));
                liveRating.setStdChange(addAndRound(liveRating.getStdChange(), ratingChange));
            }
        }
        liveRatingRepository.save(liveRating);
    }

    private String extractMatch(String text, Pattern pattern, int group) {
        Matcher m = pattern.matcher(text);
        if (m.find()) return m.group(group);
        return null;
    }


    public static double addAndRound(Double current, double change) {
        double val = (current != null ? current : 0.0) + change;
        return Math.round(val * 10.0) / 10.0;
    }
}
