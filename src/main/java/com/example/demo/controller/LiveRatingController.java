package com.example.demo.controller;

import com.example.demo.dto.TopRatingResponseDTO;
import com.example.demo.dto.TopRatingsResponseDTO;
import com.example.demo.service.LiveRatingCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class LiveRatingController {
    private static final String DEFAULT_SEARCH = "";
    private final LiveRatingCacheService liveRatingCacheService;


    public LiveRatingController(LiveRatingCacheService liveRatingCacheService) {
        this.liveRatingCacheService = liveRatingCacheService;
    }

    @GetMapping("/top-ratings")
    public ResponseEntity<TopRatingsResponseDTO> getTopRatings() {
        TopRatingResponseDTO stdRatings = liveRatingCacheService.findStdRatings(0, 100, DEFAULT_SEARCH);
        TopRatingResponseDTO rapidRatings = liveRatingCacheService.findRapidRatings(0, 100, DEFAULT_SEARCH);
        TopRatingResponseDTO blitzRatings = liveRatingCacheService.findBlitzRatings(0, 100, DEFAULT_SEARCH);
        TopRatingsResponseDTO response = new TopRatingsResponseDTO(stdRatings, rapidRatings, blitzRatings);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/std-ratings")
    public ResponseEntity<TopRatingResponseDTO> getStdRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "ALL") String country,
            @RequestParam(defaultValue = "") String search) {
        if(!Objects.equals(country, "ALL"))
            return ResponseEntity.ok(liveRatingCacheService.findStdRatingsByCountry(country, page, size, search));
        return ResponseEntity.ok(liveRatingCacheService.findStdRatings(page, size, search));
    }

    @GetMapping("/rapid-ratings")
    public ResponseEntity<TopRatingResponseDTO> getRapidRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "ALL") String country,
            @RequestParam(defaultValue = "") String search
    ) {
        if(!Objects.equals(country, "ALL"))
            return ResponseEntity.ok(liveRatingCacheService.findRapidRatingsByCountry(country, page, size, search));
        return ResponseEntity.ok(liveRatingCacheService.findRapidRatings(page, size, search));
    }

    @GetMapping("/blitz-ratings")
    public ResponseEntity<TopRatingResponseDTO> getBlitzRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "ALL") String country,
            @RequestParam(defaultValue = "") String search) {
        if(!Objects.equals(country, "ALL"))
            return ResponseEntity.ok(liveRatingCacheService.findBlitzRatingsByCountry(country, page, size, search));
        return ResponseEntity.ok(liveRatingCacheService.findBlitzRatings(page, size, search));
    }
}
