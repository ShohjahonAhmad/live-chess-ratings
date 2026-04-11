package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.utils.EloCalculator;
import com.example.demo.utils.Result;
import com.example.demo.utils.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;


import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LiveGameStreamService {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\[Date \"(\\d{4}\\.\\d{2}\\.\\d{2})\"\\]");
    private static final Pattern ID_PATTERN = Pattern.compile("\\[(?:Site|GameURL) \".*/([^/\"{}]+)\"\\]");
    private static final Pattern WHITE_FIDE_PATTERN = Pattern.compile("\\[WhiteFideId \"(\\d+)\"\\]");
    private static final Pattern BLACK_FIDE_PATTERN = Pattern.compile("\\[BlackFideId \"(\\d+)\"\\]");
    private static final Pattern RESULT_PATTERN = Pattern.compile("\\[Result \"([^\"]+)\"\\]");
    private static final Pattern WHITE_RATING = Pattern.compile("\\[WhiteElo \"(\\d+)\"\\]");
    private static final Pattern BLACK_RATING = Pattern.compile("\\[BlackElo \"(\\d+)\"\\]");
    private static final Pattern TIME_CONTROL_PATTERN = Pattern.compile("\\[TimeControl \"([^\"]+)\"\\]");

    ConcurrentHashMap<String, Disposable> activeStreams = new ConcurrentHashMap<>();
    private final WebClient webClient;
    private final BroadcastRoundRepository broadcastRoundRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final RatingRepository ratingRepository;
    private final LiveRatingRepository liveRatingRepository;

    public LiveGameStreamService(WebClient webClient, BroadcastRoundRepository broadcastRoundRepository, GameRepository gameRepository, PlayerRepository playerRepository, RatingRepository ratingRepository, LiveRatingRepository liveRatingRepository) {
        this.webClient = webClient;
        this.broadcastRoundRepository = broadcastRoundRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.ratingRepository = ratingRepository;
        this.liveRatingRepository = liveRatingRepository;
    }

    @Scheduled(fixedDelay = 60000) // Changed to 1 minute to avoid rate limits
    public void refreshStreams() {
        List<BroadcastRound> rounds = broadcastRoundRepository.findByStatus(Status.ONGOING);

        for(BroadcastRound round : rounds){
            if(!activeStreams.containsKey(round.getId())){
                if (activeStreams.size() >= 4) {
                    System.out.println("Lichess API stream limit reached. Staying at 4 concurrent streams max.");
                    break;
                }

                StringBuilder pgnBuffer = new StringBuilder();
                Disposable stream = webClient.get()
                        .uri("/api/stream/broadcast/round/" + round.getId() + ".pgn")
                        .retrieve()
                        .bodyToFlux(String.class)
                        .doFinally(signalType -> {
                            activeStreams.remove(round.getId());
                            // Muted to avoid spamming the console on natural closures
                             System.out.println("Removed stream for round " + round.getName() + " from active streams.");
                        })
                        .subscribe(line -> {
                            // Process each game line for this round
                            pgnBuffer.append(line).append("\n");

                            if(line.trim().isEmpty() && pgnBuffer.length() > 5) {
                                String completePgn = pgnBuffer.toString().trim();
                                
                                // If the processed string DOES NOT end with a bracket ']', 
                                // it means we've successfully buffered past the headers and captured the move text/result!
                                if (!completePgn.endsWith("]")) {
                                    processPgn(completePgn, round);
                                    pgnBuffer.setLength(0);
                                }
                            }

                        }, error -> {
                            // Handle errors in the stream
                            System.err.println("Error in game stream for round " + round.getName() + ": " + error.getMessage());
                        }, () -> {
                            // Stream completed
                            System.out.println("Game stream for round " + round.getName() + " completed.");
                        });
                
                activeStreams.put(round.getId(), stream);
            }
        }
    }

    private String extractMatch(String text, Pattern pattern, int group) {
        Matcher m = pattern.matcher(text);
        if (m.find()) return m.group(group);
        return null;
    }

    private void processPgn(String pgn, BroadcastRound round) {
        String result = extractMatch(pgn, RESULT_PATTERN, 1);

        if(result == null || result.contains("*")) return;
        String gameId = extractMatch(pgn, ID_PATTERN, 1);

        if(gameId == null || gameRepository.existsById(gameId)) return;

        Tournament tournament = round.getTournament();

        String date = extractMatch(pgn, DATE_PATTERN, 1);
        String whiteFideId = extractMatch(pgn, WHITE_FIDE_PATTERN, 1);
        String blackFideId = extractMatch(pgn, BLACK_FIDE_PATTERN, 1);

        String whiteRatingPgn = extractMatch(pgn, WHITE_RATING, 1);
        short whiteRating = Short.parseShort(whiteRatingPgn != null ? whiteRatingPgn : "0");
        String blackRatingPgn = extractMatch(pgn, BLACK_RATING, 1);
        short blackRating = Short.parseShort(blackRatingPgn != null ? blackRatingPgn : "0");
        String timeControl = extractMatch(pgn, TIME_CONTROL_PATTERN, 1);

        System.out.println(activeStreams.keySet());
        System.out.println("Received game for round " + round.getName() + ": " + gameId + " (" + date + ") " + whiteFideId + " vs " + blackFideId + " [" + result + ", " + timeControl + "]");

        Game game = new Game();

        game.setId(gameId);
        game.setDate(date != null ? LocalDate.parse(date.replace(".", "-")) : null); //
        game.setWhiteFideId(whiteFideId != null ? Long.parseLong(whiteFideId) : null);
        game.setBlackFideId(blackFideId != null ? Long.parseLong(blackFideId) : null);
        Result resultEnum = "1-0".equals(result) ? Result.WIN : "0-1".equals(result) ? Result.LOSS : Result.DRAW;
        game.setResult(resultEnum);
        game.setWhiteRating(whiteRating);
        game.setBlackRating(blackRating);
        game.setRound(round);

        EloCalculator eloCalculator = new EloCalculator();
        Player whitePlayer = null;
        if(game.getWhiteFideId() != null) {
            whitePlayer = playerRepository.findById(game.getWhiteFideId()).orElse(null);
        }
        Player blackPlayer = null;
        if(game.getBlackFideId() != null) {
            blackPlayer = playerRepository.findById(game.getBlackFideId()).orElse(null);
        }

        String tcType = "std";
        if (timeControl != null) {
            try {
                double timeValue = Double.parseDouble(timeControl.split(" ")[0]);
                tcType = eloCalculator.findTimeControlType(timeValue);
            } catch (Exception e) {
                // Ignore parse errors safely
            }
            try {
                double timeValue = Double.parseDouble(timeControl.split("\\+")[0]);
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
                        liveRating.setBlitzRating(liveRating.getBlitzRating() != null ? liveRating.getBlitzRating() + game.getWhiteRatingChange() : game.getWhiteRatingChange());
                        liveRating.setBlitzChange((liveRating.getBlitzChange() != null ? liveRating.getBlitzChange() : 0.0) + game.getWhiteRatingChange());
                    }
                    case "rapid" -> {
                        liveRating.setRapidRating(liveRating.getRapidRating() != null ? liveRating.getRapidRating() + game.getWhiteRatingChange() : game.getWhiteRatingChange());
                        liveRating.setRapidChange((liveRating.getRapidChange() != null ? liveRating.getRapidChange() : 0.0) + game.getWhiteRatingChange());
                    }
                    case "std"   -> {
                        liveRating.setStdRating(liveRating.getStdRating() != null ? liveRating.getStdRating() + game.getWhiteRatingChange() : game.getWhiteRatingChange());
                        liveRating.setStdChange((liveRating.getStdChange() != null ? liveRating.getStdChange() : 0.0) + game.getWhiteRatingChange());
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
                        liveRating.setBlitzRating(liveRating.getBlitzRating() != null ? liveRating.getBlitzRating() + game.getBlackRatingChange() : game.getBlackRatingChange());
                        liveRating.setBlitzChange((liveRating.getBlitzChange() != null ? liveRating.getBlitzChange() : 0.0) + game.getBlackRatingChange());
                    }
                    case "rapid" -> {
                        liveRating.setRapidRating(liveRating.getRapidRating() != null ? liveRating.getRapidRating() + game.getBlackRatingChange() : game.getBlackRatingChange());
                        liveRating.setRapidChange((liveRating.getRapidChange() != null ? liveRating.getRapidChange() : 0.0) + game.getBlackRatingChange());
                    }
                    case "std"   -> {
                        liveRating.setStdRating(liveRating.getStdRating() != null ? liveRating.getStdRating() + game.getBlackRatingChange() : game.getBlackRatingChange());
                        liveRating.setStdChange((liveRating.getStdChange() != null ? liveRating.getStdChange() : 0.0) + game.getBlackRatingChange());
                    }
                }
                liveRatingRepository.save(liveRating);
            }
        }

        if(whitePlayer == null && blackPlayer == null) return;

        gameRepository.save(game);
    }
}
