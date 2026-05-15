package com.example.demo.service;

import com.example.demo.dto.TopRatingDTO;
import com.example.demo.dto.TopRatingDTOResponse;
import com.example.demo.repository.LiveRatingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LiveRatingCacheService {
    private static final Logger logger = LoggerFactory.getLogger(LiveRatingCacheService.class);

    private final LiveRatingRepository liveRatingRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private LiveRatingCacheService self;

    public LiveRatingCacheService(LiveRatingRepository liveRatingRepository, ObjectMapper objectMapper) {
        this.liveRatingRepository = liveRatingRepository;
        this.objectMapper = objectMapper;
    }

    @Cacheable(value = "std")
    public List<TopRatingDTOResponse> getTop300StdRatings() {
        return mapToResponse(liveRatingRepository.findTop300StdPlayers(), "std");
    }

    @CachePut(value = "std")
    public List<TopRatingDTOResponse> refreshTop300StdRatings() {
        return mapToResponse(liveRatingRepository.findTop300StdPlayers(), "std");
    }

    @Cacheable(value = "rapid")
    public List<TopRatingDTOResponse> getTop300RapidRatings() {
        return mapToResponse(liveRatingRepository.findTop300RapidPLayers(), "rapid");
    }

    @CachePut(value = "rapid")
    public List<TopRatingDTOResponse> refreshTop300RapidRatings() {
        return mapToResponse(liveRatingRepository.findTop300RapidPLayers(), "rapid");
    }

    @Cacheable(value = "blitz")
    public List<TopRatingDTOResponse> getTop300BlitzRatings() {
        return mapToResponse(liveRatingRepository.findTop300BlitzPLayers(), "blitz");
    }

    @CachePut(value = "blitz")
    public List<TopRatingDTOResponse> refreshTop300BlitzRatings() {
        return mapToResponse(liveRatingRepository.findTop300BlitzPLayers(), "blitz");
    }

    private List<TopRatingDTOResponse> mapToResponse(List<TopRatingDTO> result, String timeControl) {
        logger.info("Fetching top 300 {} ratings from db", timeControl);

        if(!result.isEmpty()){
            logger.debug("Fetched top 1 {} player: {} with {} rating", timeControl, result.getFirst().getFideId(), result.getFirst().getRating());
        }

        return result.stream().map(dto -> {
            try {
                return TopRatingDTOResponse.from(dto, objectMapper);
            } catch (JsonProcessingException e) {
                logger.error("Error parsing recent games JSON for top {} ratings", timeControl, e);
                throw new RuntimeException("Error parsing recent games JSON for top " + timeControl + " ratings", e);
            }
        }).toList();
    }

    // Refresh all caches every 5 minutes
    @Scheduled(fixedDelay = 300000)
    public void refreshAllCaches() {
        logger.info("Refreshing top ratings cache...");
        self.refreshTop300StdRatings();
        self.refreshTop300RapidRatings();
        self.refreshTop300BlitzRatings();
    }
}
