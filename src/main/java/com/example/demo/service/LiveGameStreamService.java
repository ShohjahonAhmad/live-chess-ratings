package com.example.demo.service;

import com.example.demo.entity.BroadcastRound;
import com.example.demo.repository.BroadcastRoundRepository;
import com.example.demo.utils.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.util.List;
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

    public LiveGameStreamService(WebClient webClient, BroadcastRoundRepository broadcastRoundRepository) {
        this.webClient = webClient;
        this.broadcastRoundRepository = broadcastRoundRepository;
    }

    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void refreshStreams() {
        List<BroadcastRound> rounds = broadcastRoundRepository.findByStatus(Status.ONGOING);

        for(BroadcastRound round : rounds){
            if(!activeStreams.containsKey(round.getId())){
                StringBuilder pgnBuffer = new StringBuilder();
                Disposable stream = webClient.get()
                        .uri("/api/stream/broadcast/round/" + round.getId() + ".pgn")
                        .retrieve()
                        .bodyToFlux(String.class)
                        .doFinally(signalType -> {
                            activeStreams.remove(round.getId());
                            System.out.println("Removed stream for round " + round.getName() + " from active streams.");
                        })
                        .subscribe(line -> {
                            // Process each game line for this round
                            pgnBuffer.append(line).append("\n");

                            if(line.trim().isEmpty() && pgnBuffer.length() > 5) {
                                String completePgn = pgnBuffer.toString().trim();
                                
                                // The magic fix: Check if we are at the empty line after headers, or after moves
                                if (completePgn.endsWith("]")) {
                                    // This is the empty line separating headers from move text. Keep buffering!
                                    return;
                                }

                                String gameId = extractMatch(completePgn, ID_PATTERN, 1);
                                String date = extractMatch(completePgn, DATE_PATTERN, 1);
                                String whiteFideId = extractMatch(completePgn, WHITE_FIDE_PATTERN, 1);
                                String blackFideId = extractMatch(completePgn, BLACK_FIDE_PATTERN, 1);
                                String result = extractMatch(completePgn, RESULT_PATTERN, 1);
                                String whiteRating = extractMatch(completePgn, WHITE_RATING, 1);
                                String blackRating = extractMatch(completePgn, BLACK_RATING, 1);
                                String timeControl = extractMatch(completePgn, TIME_CONTROL_PATTERN, 1);

                                System.out.println("Received game for round " + round.getName() + ": " + gameId + " (" + date + ") " + whiteFideId + " vs " + blackFideId + " [" + result + ", " + timeControl + "]");
                                pgnBuffer.setLength(0);
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

}
