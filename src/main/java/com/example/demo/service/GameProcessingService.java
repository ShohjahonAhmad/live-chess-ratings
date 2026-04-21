package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.LiveRatingRepository;
import com.example.demo.repository.PlayerRepository;
import com.example.demo.utils.EloCalculator;
import com.example.demo.utils.Result;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GameProcessingService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final LiveRatingRepository liveRatingRepository;


    public GameProcessingService(GameRepository gameRepository, PlayerRepository playerRepository, LiveRatingRepository liveRatingRepository) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.liveRatingRepository = liveRatingRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(GameProcessingService.class);
    private static final Pattern ID_PATTERN = Pattern.compile("\\[(?:Site|GameURL) \".*/([^/\"{}]+)\"\\]");
    private static final Pattern WHITE_FIDE_PATTERN = Pattern.compile("\\[WhiteFideId \"(\\d+)\"\\]");
    private static final Pattern BLACK_FIDE_PATTERN = Pattern.compile("\\[BlackFideId \"(\\d+)\"\\]");
    private static final Pattern RESULT_PATTERN = Pattern.compile("\\[Result \"([^\"]+)\"\\]");
    private static final Pattern WHITE_RATING = Pattern.compile("\\[WhiteElo \"(\\d+)\"\\]");
    private static final Pattern BLACK_RATING = Pattern.compile("\\[BlackElo \"(\\d+)\"\\]");
    private static final Pattern TIME_CONTROL_PATTERN = Pattern.compile("\\[TimeControl \"([^\"]+)\"\\]");


    @Transactional
    public void processPgn(String pgn, BroadcastRound round, Map<String, Boolean> ignoredGames) {
        String result = extractMatch(pgn, RESULT_PATTERN, 1);

        if(result == null || result.contains("*")) return;
        String gameId = extractMatch(pgn, ID_PATTERN, 1);

        if(gameId == null || ignoredGames.containsKey(gameId) || gameRepository.existsById(gameId)) return;

        Tournament tournament = round.getTournament();

        LocalDate today = LocalDate.now();

        String whiteFideId = extractMatch(pgn, WHITE_FIDE_PATTERN, 1);
        String blackFideId = extractMatch(pgn, BLACK_FIDE_PATTERN, 1);

        Player whitePlayer = null;
        if(whiteFideId != null) {
            whitePlayer = playerRepository.findById(Long.valueOf(whiteFideId)).orElse(null);
        }
        Player blackPlayer = null;
        if(blackFideId != null) {
            blackPlayer = playerRepository.findById(Long.valueOf(blackFideId)).orElse(null);
        }

        if(whitePlayer == null && blackPlayer == null) {
            ignoredGames.put(gameId, true);
            return;
        }

        String whiteRatingPgn = extractMatch(pgn, WHITE_RATING, 1);
        short whiteRating = Short.parseShort(whiteRatingPgn != null ? whiteRatingPgn : "0");
        String blackRatingPgn = extractMatch(pgn, BLACK_RATING, 1);
        short blackRating = Short.parseShort(blackRatingPgn != null ? blackRatingPgn : "0");
        String timeControl = extractMatch(pgn, TIME_CONTROL_PATTERN, 1);

        Game game = new Game();

        game.setId(gameId);
        game.setDate(today);
        game.setWhiteFideId(whiteFideId != null ? Long.parseLong(whiteFideId) : null);
        game.setBlackFideId(blackFideId != null ? Long.parseLong(blackFideId) : null);
        Result resultEnum = "1-0".equals(result) ? Result.WIN : "0-1".equals(result) ? Result.LOSS : Result.DRAW;
        game.setResult(resultEnum);
        game.setWhiteRating(whiteRating);
        game.setBlackRating(blackRating);
        game.setRound(round);

        EloCalculator eloCalculator = new EloCalculator();

        String tcType = "std";
        if (timeControl != null) {
            try {
                double timeValue = getFirstNumberFromString(timeControl);
                tcType = eloCalculator.findTimeControlType(timeValue);
            } catch (Exception e) {
                // Ignore parse errors safely
            }
        } else {
            // If time control is missing, try to infer from tournament settings
            if (tournament.getTc() != null) {
                try {
                    double timeValue = Double.parseDouble(tournament.getTc().split(" ")[0]);
                    tcType = eloCalculator.findTimeControlType(timeValue);
                } catch (Exception e) {
                    // fallback to std if parsing fails
                    tcType = "std";
                }
            }
        }

        if(whitePlayer != null && tcType != null) {
            int k = 0;
            switch (tcType) {
                case "blitz" -> k = whitePlayer.getBlitzK() != null ? whitePlayer.getBlitzK() : 20;
                case "rapid" -> k = whitePlayer.getRapidK() != null ? whitePlayer.getRapidK() : 20;
                case "std"   -> k = whitePlayer.getStdK() != null ? whitePlayer.getStdK() : 20;
            }

            double actualScore = resultEnum == Result.WIN ? 1.0 : (resultEnum == Result.LOSS ? 0.0 : 0.5);
            double expectedScore = eloCalculator.calculateExpectedScore(whiteRating, blackRating);
            game.setWhiteRatingChange(eloCalculator.calculateRatingChange(k, actualScore, expectedScore));

            LiveRating liveRating = liveRatingRepository.findById(whitePlayer.getFideId()).orElse(null);
            if (liveRating != null) {
                switch (tcType) {
                    case "blitz" -> {
                        liveRating.setBlitzRating(addAndRound(liveRating.getBlitzRating(), game.getWhiteRatingChange()));
                        liveRating.setBlitzChange(addAndRound(liveRating.getBlitzChange(), game.getWhiteRatingChange()));
                    }
                    case "rapid" -> {
                        liveRating.setRapidRating(addAndRound(liveRating.getRapidRating(), game.getWhiteRatingChange()));
                        liveRating.setRapidChange(addAndRound(liveRating.getRapidChange(), game.getWhiteRatingChange()));
                    }
                    case "std"   -> {
                        liveRating.setStdRating(addAndRound(liveRating.getStdRating(), game.getWhiteRatingChange()));
                        liveRating.setStdChange(addAndRound(liveRating.getStdChange(), game.getWhiteRatingChange()));
                    }
                }
                liveRatingRepository.save(liveRating);
            }
        }

        if(blackPlayer != null && tcType != null) {
            int k = 0;
            switch (tcType) {
                case "blitz" -> k = blackPlayer.getBlitzK() != null ? blackPlayer.getBlitzK() : 20;
                case "rapid" -> k = blackPlayer.getRapidK() != null ? blackPlayer.getRapidK() : 20;
                case "std"   -> k = blackPlayer.getStdK() != null ? blackPlayer.getStdK() : 20;
            }

            double actualScore = resultEnum == Result.LOSS ? 1.0 : (resultEnum == Result.WIN ? 0.0 : 0.5);
            double expectedScore = eloCalculator.calculateExpectedScore(blackRating, whiteRating);
            game.setBlackRatingChange(eloCalculator.calculateRatingChange(k, actualScore, expectedScore));

            LiveRating liveRating = liveRatingRepository.findById(blackPlayer.getFideId()).orElse(null);
            if (liveRating != null) {
                switch (tcType) {
                    case "blitz" -> {
                        liveRating.setBlitzRating(addAndRound(liveRating.getBlitzRating(), game.getBlackRatingChange()));
                        liveRating.setBlitzChange(addAndRound(liveRating.getBlitzChange(), game.getBlackRatingChange()));
                    }
                    case "rapid" -> {
                        liveRating.setRapidRating(addAndRound(liveRating.getRapidRating(), game.getBlackRatingChange()));
                        liveRating.setRapidChange(addAndRound(liveRating.getRapidChange(), game.getBlackRatingChange()));
                    }
                    case "std"   -> {
                        liveRating.setStdRating(addAndRound(liveRating.getStdRating(), game.getBlackRatingChange()));
                        liveRating.setStdChange(addAndRound(liveRating.getStdChange(), game.getBlackRatingChange()));
                    }
                }
                liveRatingRepository.save(liveRating);
            }
        }

        gameRepository.save(game);
        logger.debug("Game {} saved: {} vs {} - Result: {}", gameId, whiteFideId, blackFideId, result);
    }

    private String extractMatch(String text, Pattern pattern, int group) {
        Matcher m = pattern.matcher(text);
        if (m.find()) return m.group(group);
        return null;
    }

    private double getFirstNumberFromString(String timeControl) {
        double result = 0;

        for(int i = 0; i < timeControl.length(); i++) {
            char c = timeControl.charAt(i);

            if(!Character.isDigit(c)) break;

            result = result * 10 + Integer.parseInt(String.valueOf(c));
        }

        return result;
    }

    private double addAndRound(Double current, double change) {
        double val = (current != null ? current : 0.0) + change;
        return Math.round(val * 10.0) / 10.0;
    }
}
