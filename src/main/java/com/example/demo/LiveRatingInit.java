package com.example.demo;

import com.example.demo.entity.LiveRating;
import com.example.demo.entity.Player;
import com.example.demo.entity.Rating;
import com.example.demo.repository.LiveRatingRepository;
import com.example.demo.repository.PlayerRepository;
import com.example.demo.repository.RatingRepository;
import com.example.demo.service.GameProcessingService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LiveRatingInit implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LiveRatingInit.class);

    private final PlayerRepository playerRepository;
    private final RatingRepository ratingRepository;
    private final LiveRatingRepository liveRatingRepository;

    public LiveRatingInit(PlayerRepository playerRepository, RatingRepository ratingRepository, LiveRatingRepository liveRatingRepository) {
        this.playerRepository = playerRepository;
        this.ratingRepository = ratingRepository;
        this.liveRatingRepository = liveRatingRepository;
    }


    @Override
    public void run(String... args) throws Exception {
        System.out.println("LiveRatingInit is executing... Current count: " + liveRatingRepository.count());
        if(liveRatingRepository.count() == 0) {
            System.out.println("Initializing live ratings...");
            
            List<Player> players = playerRepository.findAll();


            for(Player player : players) {
                Rating latestRating = ratingRepository.findTopByPlayerFideIdOrderByPeriodDesc(player.getFideId()).orElse(null);
                if(latestRating == null) continue;

                LiveRating liveRating = getLiveRating(player, latestRating);

                liveRatingRepository.save(liveRating);
            }


            System.out.println("Live ratings initialized for " + players.size() + " players.");
        }
    }

    private static @NonNull LiveRating getLiveRating(Player player, Rating latestRating) {
        LiveRating liveRating = new LiveRating();

        liveRating.setPlayer(player);
        
        if (latestRating.getBlitzRating() != null) liveRating.setBlitzRating(latestRating.getBlitzRating().doubleValue());
        if (latestRating.getRapidRating() != null) liveRating.setRapidRating(latestRating.getRapidRating().doubleValue());
        if (latestRating.getStdRating() != null) liveRating.setStdRating(latestRating.getStdRating().doubleValue());
        
        liveRating.setBlitzChange(0.0);
        liveRating.setRapidChange(0.0);
        liveRating.setStdChange(0.0);
        return liveRating;
    }
}
