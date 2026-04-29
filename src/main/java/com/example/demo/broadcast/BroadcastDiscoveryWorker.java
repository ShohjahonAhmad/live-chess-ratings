package com.example.demo.broadcast;

import com.example.demo.dto.BroadcastDTO;
import com.example.demo.dto.BroadcastRoundDTO;
import com.example.demo.entity.BroadcastRound;
import com.example.demo.entity.Tournament;
import com.example.demo.repository.BroadcastRoundRepository;
import com.example.demo.repository.TournamentRepository;
import com.example.demo.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

@Component
public class BroadcastDiscoveryWorker {
    final private static byte MAX_BROADCASTS = 100; //Default => 20, Max => 100
    private static final Logger logger = LoggerFactory.getLogger(BroadcastDiscoveryWorker.class);

    final private WebClient webClient;
    final private TournamentRepository tournamentRepository;
    final private BroadcastRoundRepository broadcastRoundRepository;

    public BroadcastDiscoveryWorker(WebClient webClient, TournamentRepository tournamentRepository, BroadcastRoundRepository broadcastRoundRepository) {
        this.webClient = webClient;
        this.tournamentRepository = tournamentRepository;
        this.broadcastRoundRepository = broadcastRoundRepository;
    }
    /// Discover Broadcast
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void discoverBroadcasts() {
        logger.info("Starting broadcast discovery...");
        
        webClient.get()
                .uri("/api/broadcast?nb=100&live=true")
                .retrieve()
                .bodyToFlux(BroadcastDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doBeforeRetry(retrySignal -> {
                            logger.warn("Retrying broadcast discovery (attempt {}/3)", retrySignal.totalRetries() + 1);
                        })
                )
                .subscribe(
                        this::processBroadcast,
                    error -> logger.error("Error discovering broadcasts: {}", error.getMessage(), error),
                    () -> logger.info("Broadcast discovery completed")
                );
    }

    private void processBroadcast(BroadcastDTO broadcastDTO) {
        logger.info("Processing broadcast for tournament: {}", broadcastDTO.tour != null && broadcastDTO.tour.id != null ? broadcastDTO.tour.id : "unknown");

        try {
            validateBroadcast(broadcastDTO);
        } catch (IllegalArgumentException e){
            return;
        }

        Tournament tournament = convertTourDTOTournament(broadcastDTO);

        Tournament existingTournament = tournamentRepository.findById(tournament.getId()).orElse(new Tournament());
        if(!tournament.equals(existingTournament)) {
            saveUpdatedTournament(existingTournament, tournament);
        }

        for(BroadcastDTO.RoundDTO roundDTO : broadcastDTO.rounds) {
            saveTournamentRound(roundDTO, existingTournament);
        }
    }

    private void validateBroadcast(BroadcastDTO broadcastDTO) {
        // Skip broadcast if critical fields are missing
        if (broadcastDTO.tour == null) {
            logger.warn("Skipping broadcast: missing critical data (tour, info, or dates)");
            throw new IllegalArgumentException();
        }

        // Skip unrated tournaments
        if(!broadcastDTO.rounds.getFirst().rated || broadcastDTO.tour.info.location.contains(".com")){
            logger.warn("Skipping {} broadcastDTO: unrated rounds", broadcastDTO.tour.name);
            throw new IllegalArgumentException();
        }
    }

    private Tournament convertTourDTOTournament(BroadcastDTO broadcast) {
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

        return tournament;
    }

    private void saveUpdatedTournament(Tournament existingTournament, Tournament updatedTournament) {
        if (existingTournament.getId() == null) {
            existingTournament.setId(updatedTournament.getId());
        }

        existingTournament.setName(updatedTournament.getName());
        existingTournament.setSlug(updatedTournament.getSlug());
        existingTournament.setDescription(updatedTournament.getDescription());
        existingTournament.setStartsAt(updatedTournament.getStartsAt());
        existingTournament.setEndsAt(updatedTournament.getEndsAt());
        existingTournament.setTc(updatedTournament.getTc());
        existingTournament.setFormat(updatedTournament.getFormat());
        existingTournament.setLocation(updatedTournament.getLocation());

        tournamentRepository.save(existingTournament);
    }

    private void validateTournamentRound(BroadcastDTO.RoundDTO roundDTO) {
        // Skip round if critical fields are missing
        if (roundDTO.id == null || roundDTO.name == null) {
            logger.warn("Skipping round: missing id or name");
            throw new IllegalArgumentException();
        }

        //Skip unrated round
        if(!roundDTO.rated) {
            logger.warn("Skipping round {}: not rated", roundDTO.id);
            throw new IllegalArgumentException();
        }
    }

    private void saveTournamentRound(BroadcastDTO.RoundDTO roundDTO, Tournament tournament) {
        logger.info("Processing round {}: {} (ongoing)", roundDTO.id != null ? roundDTO.id : "unknown", roundDTO.ongoing);

        try {
            validateTournamentRound(roundDTO);
        } catch (IllegalArgumentException e){
            return;
        }

        BroadcastRound round = convertBroadcastRoundDTOBroadcastRound(roundDTO);
        round.setTournament(tournament);

        BroadcastRound existing = broadcastRoundRepository.findById(round.getId()).orElse(new BroadcastRound());
        if(!round.equals(existing)){
            saveUpdatedBroadcastRound(existing, round, tournament);
        }
    }

    public BroadcastRound convertBroadcastRoundDTOBroadcastRound(BroadcastDTO.RoundDTO roundDTO) {
        BroadcastRound round = new BroadcastRound();

        round.setId(roundDTO.id);
        round.setName(roundDTO.name);

        if(roundDTO.slug != null){
            round.setSlug(roundDTO.slug);
        }

        // Deciding game status
        if(roundDTO.finished) round.setStatus(Status.FINISHED);
        else if(roundDTO.ongoing) round.setStatus(Status.ONGOING);
        else if (roundDTO.startsAt != null && roundDTO.startsAt < Instant.now().toEpochMilli()) round.setStatus(Status.ONGOING);
        else round.setStatus(Status.UNSTARTED);

        if(roundDTO.startsAt != null) {
            round.setStartsAt(Instant.ofEpochMilli(roundDTO.startsAt));
        }

        if(roundDTO.finishedAt != null){
            round.setEndsAt(Instant.ofEpochMilli(roundDTO.finishedAt));
        }

        return round;
    }

    public void saveUpdatedBroadcastRound(BroadcastRound existing, BroadcastRound updated, Tournament tournament) {
        if (existing.getId() == null) {
            existing.setId(updated.getId());
        }

        existing.setName(updated.getName());
        if(updated.getSlug() != null) {
            existing.setSlug(updated.getSlug());
        }
        existing.setStatus(updated.getStatus());
        if(updated.getStartsAt() != null && updated.getStartsAt().toEpochMilli() > 0) {
            existing.setStartsAt(updated.getStartsAt());
        }
        if(updated.getEndsAt() != null && updated.getEndsAt().toEpochMilli() > 0) {
            existing.setEndsAt(updated.getEndsAt());
        }
        existing.setTournament(tournament);

        broadcastRoundRepository.save(existing);
    }
    /// Discover Broadcast (end)


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
                            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                            .retrieve()
                            .bodyToMono(BroadcastRoundDTO.class)
                            .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                                    .doBeforeRetry(retrySignal -> {
                                        logger.warn("Retrying fetch status of round (attempt {}/2)", retrySignal.totalRetries() + 1);
                                    }))
                            .doOnNext(dto -> {
                                if (dto.round != null) {
                                    if (dto.round.finished) {
                                        round.setStatus(Status.FINISHED);
                                        if (dto.round.finishedAt != null && dto.round.finishedAt > 0) {
                                            round.setEndsAt(Instant.ofEpochMilli(dto.round.finishedAt));
                                        }
                                        broadcastRoundRepository.save(round);
                                    }
                                }
                            })
                            .doOnError(error -> {
                                        logger.error("Failed to fetch round {} for cleanup: {}", round.getId(), error.getMessage(), error);
                                    }
                            )
                            .onErrorResume(e -> reactor.core.publisher.Mono.empty()); // Swallow error so the loop continues
                })
                .blockLast();
    }
}
