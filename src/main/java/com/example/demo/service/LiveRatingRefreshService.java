package com.example.demo.service;

import com.example.demo.entity.Game;
import com.example.demo.entity.LiveRating;
import com.example.demo.entity.Rating;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.LiveRatingRepository;
import com.example.demo.repository.RatingRepository;
import com.example.demo.utils.TimeControl;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LiveRatingRefreshService {
    private static final Logger logger = LoggerFactory.getLogger(LiveRatingRefreshService.class);

    private final LiveRatingRepository liveRatingRepository;
    private final RatingRepository ratingRepository;
    private final GameRepository gameRepository;

    public LiveRatingRefreshService(LiveRatingRepository liveRatingRepository, RatingRepository ratingRepository, GameRepository gameRepository) {
        this.liveRatingRepository = liveRatingRepository;
        this.ratingRepository = ratingRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional
public void refreshLiveRatings(LocalDate period) {
            logger.info("Starting live ratings refresh for period: {}", period);
            liveRatingRepository.deleteAll();

            List<Rating> monthlyRatings = ratingRepository.findByPeriod(period);

            for(Rating rating : monthlyRatings) {
                LiveRating liveRating = new LiveRating();
                liveRating.setPlayer(rating.getPlayer());
                liveRating.setStdRating(rating.getStdRating() != null ? (double) rating.getStdRating() : 0.0);
                liveRating.setRapidRating(rating.getRapidRating() != null ? (double) rating.getRapidRating() : 0.0);
                liveRating.setBlitzRating(rating.getBlitzRating() != null ? (double) rating.getBlitzRating() : 0.0);
                liveRatingRepository.save(liveRating);
            }

            recalculateFromGames();
            logger.info("Live ratings refresh completed successfully for period: {}", period);
    }

    private void recalculateFromGames() {
        List<Game> games = gameRepository.findAll();
        logger.info("Recalculating from {} games", games.size());

        for(Game game : games) {
            LiveRating whiteLiveRating = liveRatingRepository.findByPlayerFideId(game.getWhiteFideId());
            LiveRating blackLiveRating = liveRatingRepository.findByPlayerFideId(game.getBlackFideId());

            if(whiteLiveRating == null && blackLiveRating == null) continue;

            if(whiteLiveRating != null && game.getWhiteRatingChange() != null) {
                switch (game.getTimeControl()){
                    case TimeControl.STD -> {
                        whiteLiveRating.setStdRating(whiteLiveRating.getStdRating() + game.getWhiteRatingChange());
                        whiteLiveRating.setStdChange(whiteLiveRating.getStdChange() + game.getWhiteRatingChange());
                    }
                    case TimeControl.RAPID -> {
                        whiteLiveRating.setRapidRating(whiteLiveRating.getRapidRating() + game.getWhiteRatingChange());
                        whiteLiveRating.setRapidChange(whiteLiveRating.getRapidChange() + game.getWhiteRatingChange());
                    }
                    case TimeControl.BLITZ -> {
                        whiteLiveRating.setBlitzRating(whiteLiveRating.getBlitzRating() + game.getWhiteRatingChange());
                        whiteLiveRating.setBlitzChange(whiteLiveRating.getBlitzChange() + game.getWhiteRatingChange() );
                    }
                }

                liveRatingRepository.save(whiteLiveRating);
            }

            if(blackLiveRating != null && game.getBlackRatingChange() != null) {
                switch (game.getTimeControl()){
                    case TimeControl.STD -> {
                        blackLiveRating.setStdRating(blackLiveRating.getStdRating() + game.getBlackRatingChange());
                        blackLiveRating.setStdChange(blackLiveRating.getStdChange() + game.getBlackRatingChange());
                    }
                    case TimeControl.RAPID -> {
                        blackLiveRating.setRapidRating(blackLiveRating.getRapidRating() + game.getBlackRatingChange());
                        blackLiveRating.setRapidChange(blackLiveRating.getRapidChange() + game.getBlackRatingChange());
                    }
                    case TimeControl.BLITZ -> {
                        blackLiveRating.setBlitzRating(blackLiveRating.getBlitzRating() + game.getBlackRatingChange());
                        blackLiveRating.setBlitzChange(blackLiveRating.getBlitzChange() + game.getBlackRatingChange());
                    }
                }

                liveRatingRepository.save(blackLiveRating);
            }
        }
    }
}
