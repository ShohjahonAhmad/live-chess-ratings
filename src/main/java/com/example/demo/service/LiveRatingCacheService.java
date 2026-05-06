package com.example.demo.service;

import com.example.demo.dto.TopRatingDTO;
import com.example.demo.repository.LiveRatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LiveRatingCacheService {
    private static final Logger logger = LoggerFactory.getLogger(LiveRatingCacheService.class);

    private final LiveRatingRepository liveRatingRepository;

    public LiveRatingCacheService(LiveRatingRepository liveRatingRepository) {
        this.liveRatingRepository = liveRatingRepository;
    }

    @Cacheable(value = "std")
    public List<TopRatingDTO> getTop300StdRatings() {
        logger.info("Fetching top 300 standard ratings from db");
        var result = liveRatingRepository.findTop300StdPlayers();
        for(TopRatingDTO dto : result) {
            logger.debug("Std player: fideId={}, name={}, rating={}, ratingChange={}, gameCount={}",
                    dto.getFideId(), dto.getName(), dto.getRating(), dto.getRatingChange(), dto.getCount());
        }
        return result;
    }

    @Cacheable(value = "rapid")
    public List<TopRatingDTO> getTop300RapidRatings() {
        logger.info("Fetching top 300 rapid ratings from db");
        var result = liveRatingRepository.findTop300RapidPLayers();
        for(TopRatingDTO dto : result) {
            logger.debug("Rapid player: fideId={}, name={}, rating={}, ratingChange={}, gameCount={}",
                    dto.getFideId(), dto.getName(), dto.getRating(), dto.getRatingChange(), dto.getCount());
        }
        return result;
    }

    @Cacheable(value = "blitz")
    public List<TopRatingDTO> getTop300BlitzRatings() {
        logger.info("Fetching top 300 blitz ratings from db");
        var result = liveRatingRepository.findTop300BlitzPLayers();
        for (TopRatingDTO dto : result) {
            logger.debug("Blitz player: fideId={}, name={}, rating={}, ratingChange={}, gameCount={}",
                    dto.getFideId(), dto.getName(), dto.getRating(), dto.getRatingChange(), dto.getCount());
        }
        return result;
    }

    // Refresh all caches every 5 minutes
    @Scheduled(fixedDelay = 300000)
    public void refreshAllCaches() {
        logger.info("Refreshing top ratings cache...");
        getTop300StdRatings();
        getTop300RapidRatings();
        getTop300BlitzRatings();
    }
}
