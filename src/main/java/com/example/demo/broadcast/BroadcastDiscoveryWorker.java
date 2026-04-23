package com.example.demo.broadcast;

import com.example.demo.dto.TopDTO;
import com.example.demo.entity.BroadcastRound;
import com.example.demo.entity.Tournament;
import com.example.demo.repository.BroadcastRoundRepository;
import com.example.demo.repository.TournamentRepository;
import com.example.demo.utils.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

@Component
public class BroadcastDiscoveryWorker {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastDiscoveryWorker.class);

    final private WebClient webClient;
    final private TournamentRepository tournamentRepository;
    final private BroadcastRoundRepository broadcastRoundRepository;

    public BroadcastDiscoveryWorker(WebClient webClient, ObjectMapper objectMapper, TournamentRepository tournamentRepository, BroadcastRoundRepository broadcastRoundRepository) {
        this.webClient = webClient;
        this.tournamentRepository = tournamentRepository;
        this.broadcastRoundRepository = broadcastRoundRepository;
    }

    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void discoverBroadcasts() {
        logger.info("Starting broadcast discovery...");
        
        webClient.get()
                .uri("/api/broadcast/top?page=1")
                .retrieve()
                .bodyToMono(TopDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doBeforeRetry(retrySignal -> {
                            logger.warn("Retrying broadcast discovery (attempt {}/3)", retrySignal.totalRetries() + 1);
                        })
                )
                .subscribe(
                    topDTO -> {
                        if (topDTO.active == null) return;

                        for(TopDTO.BroadcastDTO broadcast : topDTO.active){
                            logger.info("Processing broadcast for tournament: {}", broadcast.tour != null ? broadcast.tour.name : "unknown");
                            if (broadcast.tour == null) {
                                logger.warn("Skipping broadcast: missing critical data (tour, info, or dates)");
                                return;
                            }

                            if(!broadcast.round.rated){
                                logger.warn("Skipping {} broadcast: unrated rounds", broadcast.tour.name);
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
                            if(!tournament.equals(existingTournament)) {
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
                            }

                            if(broadcast.round != null ) {
                                saveTournamentRound(broadcast, existingTournament);
                            }
                        }

                    },
                    error -> logger.error("Error discovering broadcasts: {}", error.getMessage(), error),
                    () -> logger.info("Broadcast discovery completed")
                );
    }

    @Scheduled(fixedDelay = 600000) // Run every 10 minutes
    public void cleanupOngoingRounds() {
        logger.info("Starting cleanup for ONGOING rounds...");
        java.util.List<BroadcastRound> ongoingRounds = broadcastRoundRepository.findByStatus(Status.ONGOING);

        reactor.core.publisher.Flux.fromIterable(ongoingRounds)
                .concatMap(round -> {
                    String tourSlug = round.getTournament() != null && round.getTournament().getSlug() != null ? round.getTournament().getSlug() : null;
                    String roundSlug = round.getSlug() != null ? round.getSlug() : null;
                    if(tourSlug == null || roundSlug == null) return reactor.core.publisher.Mono.empty();

                    return webClient.get()
                            .uri("/api/broadcast/{tourSlug}/{roundSlug}/{roundId}", tourSlug, roundSlug, round.getId())
                            .retrieve()
                            .bodyToMono(TopDTO.BroadcastDTO.class)
                            .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                                    .doBeforeRetry(retrySignal -> {
                                        logger.warn("Retrying fetch status of round (attempt {}/2)", retrySignal.totalRetries() + 1);
                                    }))
                            .doOnNext(dto -> {
                                if (dto.round != null) {
                                    if (dto.round.finished) {
                                        logger.debug("Round {} (ID: {}) has finished, updating status.", round.getName(), round.getId());
                                        round.setStatus(Status.FINISHED);
                                        if (dto.round.finishedAt != null && dto.round.finishedAt > 0) {
                                            round.setEndsAt(Instant.ofEpochMilli(dto.round.finishedAt));
                                        }
                                        broadcastRoundRepository.save(round);
                                    }
                                }
                            })
                            .doOnError(error -> logger.error("Failed to fetch round {} for cleanup after retries: {}", round.getId(), error.getMessage()))
                            .onErrorResume(e -> reactor.core.publisher.Mono.empty()); // Swallow error so the loop continues
                })
                .blockLast();
    }

    private void saveTournamentRound(TopDTO.BroadcastDTO broadcast, Tournament tournament) {
            // Skip round if critical fields are missing
            TopDTO.BroadcastDTO.RoundDTO round = broadcast.round;
            logger.info("Processing round: {}, {}", round.name != null ? round.name : "unknown", round.ongoing);
            if(!round.rated) {
                logger.warn("Skipping round {}: not rated", round.name != null ? round.name : "unknown");
                return;
            }
            if (round.id == null || round.name == null) {
                logger.warn("Skipping round: missing id or name");
                return;
            }

            BroadcastRound roundDb = new BroadcastRound();
            roundDb.setId(round.id);
            roundDb.setName(round.name);

            if(round.slug != null){
                roundDb.setSlug(round.slug);
            }

            roundDb.setStatus(round.finished ? Status.FINISHED : round.ongoing ? Status.ONGOING : Status.UNSTARTED);

            if(round.startsAt != null) {
                roundDb.setStartsAt(Instant.ofEpochMilli(round.startsAt));
            }

            if(round.finishedAt != null){
                roundDb.setEndsAt(Instant.ofEpochMilli(round.finishedAt));
            }

            roundDb.setTournament(tournament);

            BroadcastRound existing = broadcastRoundRepository.findById(round.id).orElse(new BroadcastRound());
            if(!roundDb.equals(existing)){
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
