package com.example.demo.broadcast;

import com.example.demo.dto.BroadcastGroupDTO;
import com.example.demo.dto.TopDTO;
import com.example.demo.dto.TopGroupDTO;
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
                            processBroadcast(broadcast);
                        }
                    },
                    error -> logger.error("Error discovering broadcasts: {}", error.getMessage(), error),
                    () -> logger.info("Broadcast discovery completed")
                );
    }

    private void processBroadcast(TopDTO.BroadcastDTO broadcastDTO) {
        logger.info("Processing broadcast for tournament: {}", broadcastDTO.tour != null && broadcastDTO.tour.id != null ? broadcastDTO.tour.id : "unknown");

        // Skip broadcast if critical fields are missing
        if (broadcastDTO.tour == null) {
            logger.warn("Skipping broadcast: missing critical data (tour, info, or dates)");
            return;
        }

        // Skip unrated tournaments
        if(!broadcastDTO.round.rated || broadcastDTO.tour.info.location.equals("Chess.com")){
            logger.warn("Skipping {} broadcastDTO: unrated rounds", broadcastDTO.tour.name);
            return;
        }

        Tournament tournament = convertTourDTOTournament(broadcastDTO);

        Tournament existingTournament = tournamentRepository.findById(tournament.getId()).orElse(new Tournament());
        if(!tournament.equals(existingTournament)) {
            saveUpdatedTournament(existingTournament, tournament);
        }

        if(broadcastDTO.round != null) {
            saveTournamentRound(broadcastDTO, existingTournament);
        }
    }

    private Tournament convertTourDTOTournament(TopDTO.BroadcastDTO broadcast) {
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

        if(broadcast.group != null && broadcast.round.ongoing) {
            findSiblingTournaments(broadcast.tour.id);
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

    private void saveTournamentRound(TopDTO.BroadcastDTO broadcast, Tournament tournament) {
        TopDTO.BroadcastDTO.RoundDTO roundDTO = broadcast.round;
        logger.info("Processing round {}: {} (ongoing)", roundDTO.id != null ? roundDTO.id : "unknown", roundDTO.ongoing);

        // Skip round if critical fields are missing
        if (roundDTO.id == null || roundDTO.name == null) {
            logger.warn("Skipping round: missing id or name");
            return;
        }

        //Skip unrated round
        if(!roundDTO.rated) {
            logger.warn("Skipping round {}: not rated", roundDTO.id);
            return;
        }

        BroadcastRound round = convertBroadcastRoundDTOBroadcastRound(roundDTO);
        round.setTournament(tournament);

        BroadcastRound existing = broadcastRoundRepository.findById(round.getId()).orElse(new BroadcastRound());
        if(!round.equals(existing)){
            saveUpdatedBroadcastRound(existing, round, tournament);
        }
    }

    public BroadcastRound convertBroadcastRoundDTOBroadcastRound(TopDTO.BroadcastDTO.RoundDTO roundDTO) {
        BroadcastRound round = new BroadcastRound();

        round.setId(roundDTO.id);
        round.setName(roundDTO.name);

        if(roundDTO.slug != null){
            round.setSlug(roundDTO.slug);
        }

        // Deciding game status
        if(roundDTO.finished) round.setStatus(Status.FINISHED);
        else if(roundDTO.ongoing) round.setStatus(Status.ONGOING);
        else if (roundDTO.startsAt < Instant.now().toEpochMilli()) round.setStatus(Status.ONGOING);
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

    public void findSiblingTournaments(String id) {
        logger.info("Finding sibling tournaments for tournament ID: {}", id);

        webClient.get()
                .uri("/api/broadcast/{id}", id)
                .retrieve()
                .bodyToMono(TopGroupDTO.class)
                .subscribe(
                        topGroupDTO -> {
                            logger.debug("Received sibling tournament data for ID {}", id);
                            processSiblingTournaments(topGroupDTO.group, id);
                        },
                        error -> logger.error("Error fetching sibling tournaments for ID {}: {}", id, error.getMessage(), error),
                        () -> logger.info("Sibling tournament fetch completed for ID {}", id)
                );
    }

    public void processSiblingTournaments(TopGroupDTO.GroupDTO group, String tournamentId) {
        //Skip if group doesn't exist
        if(group == null || group.tours == null){
            logger.warn("No group date found for tournament: {}", tournamentId);
            return;
        }

        reactor.core.publisher.Flux.fromIterable(group.tours)
                .concatMap(tour ->
                    reactor.core.publisher.Mono.fromRunnable(() -> fetchTournament(tour.id))
                            .delaySubscription(Duration.ofMillis(500)) // 0.5 s between each request
                )
                .subscribe();
    }

    public void fetchTournament(String tournamentId) {
        logger.debug("Processing sibling tournament with ID: {}", tournamentId);

        webClient.get()
                .uri("/api/broadcast/{id}", tournamentId)
                .retrieve()
                .bodyToMono(BroadcastGroupDTO.class)
                .subscribe(
                        this::processSiblingTournament,
                        error -> logger.error("Error processing sibling tournament ID {}: {}", tournamentId, error.getMessage(), error),
                        () -> logger.info("Sibling tournament processing completed for ID {}", tournamentId)
                );

    }

    public void processSiblingTournament(BroadcastGroupDTO broadcast) {
        logger.info("Processing sibling broadcast for tournament: {}", (broadcast.tour != null && broadcast.tour.id != null) ? broadcast.tour.id : "unknown");

        //Skip sibling tournament if critical fields are missing
        if (broadcast.tour == null || broadcast.tour.info.location.equals("Chess.com")) {
            logger.warn("Skipping sibling broadcast: missing critical data (tour, info, or dates)");
            return;
        }

        Tournament tournament = convertTourDTOTournament(broadcast);

        Tournament existingTournament = tournamentRepository.findById(tournament.getId()).orElse(new Tournament());
        if(!tournament.equals(existingTournament)) {
            saveUpdatedTournament(existingTournament, tournament);
        }

        for(BroadcastGroupDTO.RoundDTO round : broadcast.rounds){
            saveTournamentRound(round, existingTournament);
        }
    }

    private Tournament convertTourDTOTournament(BroadcastGroupDTO broadcast) {
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

    private void saveTournamentRound(BroadcastGroupDTO.RoundDTO roundDTO, Tournament tournament) {
        logger.info("Processing sibling round {}: {} (ongoing)", roundDTO.id != null ? roundDTO.id : "unknown", roundDTO.ongoing);

        // Skip round if critical fields are missing
        if (roundDTO.id == null || roundDTO.name == null) {
            logger.warn("Skipping sibling round: missing id or name");
            return;
        }

        //Skip unrated round
        if(!roundDTO.rated) {
            logger.warn("Skipping sibling round {}: not rated", roundDTO.id);
            return;
        }

        BroadcastRound round = convertBroadcastRoundDTOBroadcastRound(roundDTO);
        round.setTournament(tournament);

        BroadcastRound existing = broadcastRoundRepository.findById(round.getId()).orElse(new BroadcastRound());
        if(!round.equals(existing)){
            saveUpdatedBroadcastRound(existing, round, tournament);
        }
    }

    public BroadcastRound convertBroadcastRoundDTOBroadcastRound(BroadcastGroupDTO.RoundDTO roundDTO) {
        BroadcastRound round = new BroadcastRound();

        round.setId(roundDTO.id);
        round.setName(roundDTO.name);

        if(roundDTO.slug != null){
            round.setSlug(roundDTO.slug);
        }

        // Deciding game status
        if(roundDTO.finished) round.setStatus(Status.FINISHED);
        else if(roundDTO.ongoing) round.setStatus(Status.ONGOING);
        else if (roundDTO.startsAt < Instant.now().toEpochMilli()) round.setStatus(Status.ONGOING);
        else round.setStatus(Status.UNSTARTED);

        if(roundDTO.startsAt != null) {
            round.setStartsAt(Instant.ofEpochMilli(roundDTO.startsAt));
        }

        if(roundDTO.finishedAt != null){
            round.setEndsAt(Instant.ofEpochMilli(roundDTO.finishedAt));
        }

        return round;
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
                            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                            .retrieve()
                            .bodyToMono(TopDTO.BroadcastDTO.class)
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
