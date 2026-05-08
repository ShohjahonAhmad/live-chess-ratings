package com.example.demo.controller;

import com.example.demo.dto.TopRatingDTO;
import com.example.demo.dto.TopRatingsResponseDTO;
import com.example.demo.service.LiveRatingCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LiveRatingController {

    private final LiveRatingCacheService liveRatingCacheService;


    public LiveRatingController(LiveRatingCacheService liveRatingCacheService) {
        this.liveRatingCacheService = liveRatingCacheService;
    }

    @GetMapping("/top-ratings")
    public ResponseEntity<TopRatingsResponseDTO> getTopRatings() {
        List<TopRatingDTO> stdRatings = liveRatingCacheService.getTop300StdRatings();
        List<TopRatingDTO> rapidRatings = liveRatingCacheService.getTop300RapidRatings();
        List<TopRatingDTO> blitzRatings = liveRatingCacheService.getTop300BlitzRatings();
        TopRatingsResponseDTO response = new TopRatingsResponseDTO(stdRatings, rapidRatings, blitzRatings);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/std-ratings")
    public ResponseEntity<List<TopRatingDTO>> getStdRatings() {
        List<TopRatingDTO> ratings = liveRatingCacheService.getTop300StdRatings();
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/rapid-ratings")
    public ResponseEntity<List<TopRatingDTO>> getRapidRatings() {
        List<TopRatingDTO> ratings = liveRatingCacheService.getTop300RapidRatings();
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/blitz-ratings")
    public ResponseEntity<List<TopRatingDTO>> getBlitzRatings() {
        List<TopRatingDTO> ratings = liveRatingCacheService.getTop300BlitzRatings();
        return ResponseEntity.ok(ratings);
    }
}
