package com.example.demo.broadcast;

import com.example.demo.dto.BroadcastDTO;
import com.example.demo.entity.BroadcastRound;
import com.example.demo.entity.Tournament;
import com.example.demo.repository.BroadcastRoundRepository;
import com.example.demo.repository.TournamentRepository;
import com.example.demo.utils.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class BroadcastDiscoveryWorker {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastDiscoveryWorker.class);

    final private WebClient webClient;
    final private ObjectMapper objectMapper;
    final private TournamentRepository tournamentRepository;
    final private BroadcastRoundRepository broadcastRoundRepository;

    public BroadcastDiscoveryWorker(WebClient webClient, ObjectMapper objectMapper, TournamentRepository tournamentRepository, BroadcastRoundRepository broadcastRoundRepository) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.tournamentRepository = tournamentRepository;
        this.broadcastRoundRepository = broadcastRoundRepository;
    }

    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void discoverBroadcasts() {
        logger.info("[INFO] Starting broadcast discovery...");
        
        webClient.get()
                .uri("/api/broadcast")
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    try {

                        logger.info(line);
                        // Parse each JSON line into BroadcastDTO object
                        BroadcastDTO tour = objectMapper.readValue(line, BroadcastDTO.class);


                        // Wraps the parsed DTO into a Mono stream so subscribe() can receive it
                        return Mono.just(tour);
                    } catch (JsonProcessingException e) {
                        // Skip malformed lines, continue processing
                        logger.warn("[WARN] Failed to parse broadcast line: {}", e.getMessage());
                        return Mono.empty();
                    }
                })
                .subscribe(
                    broadcast -> {
                        // Validate critical nested data before processing
                        if (broadcast.tour == null) {
                            logger.warn("[WARN] Skipping broadcast: missing critical data (tour, info, or dates)");
                            return;
                        }

                        Tournament tournament = new Tournament();
                        if(broadcast.tour.id != null){
                            tournament.setId(broadcast.tour.id);
                        }
                        if(broadcast.tour.name != null){
                            tournament.setName(broadcast.tour.name);
                        }
                        if(broadcast.tour.slug != null){
                            tournament.setSlug(broadcast.tour.slug);
                        }
                        if(broadcast.tour.description != null){
                            tournament.setDescription(broadcast.tour.description);
                        }
                        if(broadcast.tour.dates != null && broadcast.tour.dates.length > 0) {
                            tournament.setStartsAt(Instant.ofEpochMilli(broadcast.tour.dates[0]));
                        }
                        if(broadcast.tour.dates != null && broadcast.tour.dates.length > 1) {
                            tournament.setEndsAt(Instant.ofEpochMilli(broadcast.tour.dates[1]));
                        }

                        if(broadcast.tour.info != null) {
                            if(broadcast.tour.info.tc != null){
                                tournament.setTc(broadcast.tour.info.tc);
                            }
                            if(broadcast.tour.info.format != null){
                                tournament.setFormat(broadcast.tour.info.format);
                            }
                            if(broadcast.tour.info.location != null){
                                tournament.setLocation(broadcast.tour.info.location);
                            }
                        }

                        Tournament existingTournament = tournamentRepository.findById(tournament.getId()).orElse(new Tournament());
                        if (existingTournament.getId() == null) {
                            existingTournament.setId(tournament.getId());
                        }
                        existingTournament.setName(tournament.getName());
                        existingTournament.setSlug(tournament.getSlug());
                        existingTournament.setDescription(tournament.getDescription());
                        existingTournament.setStartsAt(tournament.getStartsAt());
                        existingTournament.setEndsAt(tournament.getEndsAt());
                        existingTournament.setTc(tournament.getTc());
                        existingTournament.setFormat(tournament.getFormat());
                        existingTournament.setLocation(tournament.getLocation());

                        tournamentRepository.save(existingTournament);

                        if(broadcast.rounds != null) {
                            saveTournamentRounds(broadcast, existingTournament);
                        }
                    },
                    error -> logger.error("[ERROR] Error discovering broadcasts: {}", error.getMessage(), error),
                    () -> logger.info("[SUCCESS] Broadcast discovery completed")
                );
    }

    private void saveTournamentRounds(BroadcastDTO broadcast, Tournament tournament) {
        for (BroadcastDTO.RoundDTO round : broadcast.rounds) {
            // Skip round if critical fields are missing
            if(!round.rated) {
                logger.warn("[WARN] Skipping round {}: not rated", round.name != null ? round.name : "unknown");
                continue;
            }
            if (round.id == null || round.name == null) {
                logger.warn("[WARN] Skipping round: missing id or name");
                continue;
            }

            BroadcastRound existing = broadcastRoundRepository.findById(round.id).orElse(new BroadcastRound());
            if (existing.getId() == null) {
                existing.setId(round.id);
            }
            
            existing.setName(round.name);
            if(round.slug != null) {
                existing.setSlug(round.slug);
            }
            existing.setStatus(round.finished ? Status.FINISHED : round.ongoing ? Status.ONGOING : Status.UNSTARTED);
            if(round.startsAt != null && round.startsAt > 0) {
                existing.setStartsAt(Instant.ofEpochMilli(round.startsAt));
            }
            if(round.finishedAt != null && round.finishedAt > 0) {
                existing.setEndsAt(Instant.ofEpochMilli(round.finishedAt));
            }
            existing.setTournament(tournament);

            broadcastRoundRepository.save(existing);
        }
    }
}
