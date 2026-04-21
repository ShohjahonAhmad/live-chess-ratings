package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LiveGameStreamService {
    private final WebClient webClient;
    private final BroadcastRoundRepository broadcastRoundRepository;
    private final GameProcessingService gameProcessingService;
    private static final Logger logger = LoggerFactory.getLogger(LiveGameStreamService.class);
    
    // LRU Cache to hold a maximum of 10,000 ignored games to prevent memory leaks
    private final Map<String, Boolean> ignoredGames = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(10000, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 10000; 
                }
            }
    );

    public LiveGameStreamService(WebClient webClient, BroadcastRoundRepository broadcastRoundRepository, GameProcessingService gameProcessingService) {
        this.webClient = webClient;
        this.broadcastRoundRepository = broadcastRoundRepository;
        this.gameProcessingService = gameProcessingService;
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
                                        gameProcessingService.processPgn(completePgn, round, ignoredGames);
                                        pgnBuffer.setLength(0);
                                    }
                                }
                            })
                            .doOnError(error -> {
                                logger.error("Error in game stream for round {}: {}", round.getName(), error.getMessage());
                            })
                            .doFinally(signalType -> {
                                logger.info("Ended stream for round {} from {} with signal: {}", round.getName(), round.getTournament().getName(), signalType);
                            })
                            .then(); // ensures we fully complete this stream before moving to the next round
                })
                .blockLast(); // Blocks the scheduled thread until all rounds are visited sequentially
    }
}
