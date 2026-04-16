package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.utils.EloCalculator;
import com.example.demo.utils.Result;
import com.example.demo.utils.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final WebClient webClient;
    private final BroadcastRoundRepository broadcastRoundRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final LiveRatingRepository liveRatingRepository;
    
    // LRU Cache to hold a maximum of 10,000 ignored games to prevent memory leaks
    private final Map<String, Boolean> ignoredGames = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(10000, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 10000; 
                }
            }
    );

    public LiveGameStreamService(WebClient webClient, BroadcastRoundRepository broadcastRoundRepository, GameRepository gameRepository, PlayerRepository playerRepository, LiveRatingRepository liveRatingRepository) {
        this.webClient = webClient;
        this.broadcastRoundRepository = broadcastRoundRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.liveRatingRepository = liveRatingRepository;
    }

    @Scheduled(fixedDelay = 60000) // Changed to 1 minute to avoid rate limits
    public void refreshStreams() {
        List<BroadcastRound> rounds = broadcastRoundRepository.findByStatus(Status.ONGOING);

        reactor.core.publisher.Flux.fromIterable(rounds)
                .concatMap(round -> {
                    StringBuilder pgnBuffer = new StringBuilder();
                    return webClient.get()
                            .uri("/api/stream/broadcast/round/" + round.getId() + ".pgn")
                            .retrieve()
                            .bodyToFlux(String.class)
                            .take(java.time.Duration.ofSeconds(5)) // Fetch for 5 seconds per round sequentially
                            .doOnNext(line -> {
                                // Process each game line for this round
                                pgnBuffer.append(line).append("\n");

                                if(line.trim().isEmpty() && pgnBuffer.length() > 5) {
                                    String completePgn = pgnBuffer.toString().trim();
                                    
                                    if (!completePgn.endsWith("]")) {
                                        processPgn(completePgn, round);
                                        pgnBuffer.setLength(0);
                                    }
                                }
                            })
                            .doOnError(error -> {
                                System.err.println("Error in game stream for round " + round.getName() + ": " + error.getMessage());
                            })
                            .doFinally(signalType -> {
                                System.out.println("Ended stream for round " + round.getName() + " from " + round.getTournament().getName() + " with signal: " + signalType);
                            })
                            .then(); // ensures we fully complete this stream before moving to the next round
                })
                .blockLast(); // Blocks the scheduled thread until all rounds are visited sequentially
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

        if(gameId == null || ignoredGames.containsKey(gameId) || gameRepository.existsById(gameId)) return;

        Tournament tournament = round.getTournament();

        String date = extractMatch(pgn, DATE_PATTERN, 1);
        String whiteFideId = extractMatch(pgn, WHITE_FIDE_PATTERN, 1);
        String blackFideId = extractMatch(pgn, BLACK_FIDE_PATTERN, 1);

        String whiteRatingPgn = extractMatch(pgn, WHITE_RATING, 1);
        short whiteRating = Short.parseShort(whiteRatingPgn != null ? whiteRatingPgn : "0");
        String blackRatingPgn = extractMatch(pgn, BLACK_RATING, 1);
        short blackRating = Short.parseShort(blackRatingPgn != null ? blackRatingPgn : "0");
        String timeControl = extractMatch(pgn, TIME_CONTROL_PATTERN, 1);

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
    }

    private double addAndRound(Double current, double change) {
        double val = (current != null ? current : 0.0) + change;
        return Math.round(val * 10.0) / 10.0;
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
}
