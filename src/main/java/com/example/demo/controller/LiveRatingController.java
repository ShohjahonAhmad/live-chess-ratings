package com.example.demo.controller;

import com.example.demo.dto.TopRatingResponseDTO;
import com.example.demo.dto.TopRatingsResponseDTO;
import com.example.demo.service.LiveRatingCacheService;
import com.example.demo.utils.SortBy;
import com.example.demo.utils.SortDirection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class LiveRatingController {
    private static final String DEFAULT_SEARCH = "";
    private static final SortBy DEFAULT_SORTING = SortBy.RATING;
    private static final SortDirection DEFAULT_SORT_DIR = SortDirection.DESC;
    private static final boolean DEFAULT_ONLY_ACTIVE = true;
    private final LiveRatingCacheService liveRatingCacheService;


    public LiveRatingController(LiveRatingCacheService liveRatingCacheService) {
        this.liveRatingCacheService = liveRatingCacheService;
    }

    @GetMapping("/top-ratings")
    public ResponseEntity<TopRatingsResponseDTO> getTopRatings() {
        TopRatingResponseDTO stdRatings = liveRatingCacheService.findStdRatings(0, 100, DEFAULT_SEARCH, DEFAULT_SORTING, DEFAULT_SORT_DIR, DEFAULT_ONLY_ACTIVE);
        TopRatingResponseDTO rapidRatings = liveRatingCacheService.findRapidRatings(0, 100, DEFAULT_SEARCH, DEFAULT_SORTING, DEFAULT_SORT_DIR, DEFAULT_ONLY_ACTIVE);
        TopRatingResponseDTO blitzRatings = liveRatingCacheService.findBlitzRatings(0, 100, DEFAULT_SEARCH, DEFAULT_SORTING, DEFAULT_SORT_DIR, DEFAULT_ONLY_ACTIVE);
        TopRatingsResponseDTO response = new TopRatingsResponseDTO(stdRatings, rapidRatings, blitzRatings);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/std-ratings")
    public ResponseEntity<TopRatingResponseDTO> getStdRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "ALL") String country,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "rating") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "true") boolean onlyActive
    ){
        SortBy sortBy = validateSort(sort);
        SortDirection direction = validateSortDirection(dir);
        if(!Objects.equals(country, "ALL"))
            return ResponseEntity.ok(liveRatingCacheService.findStdRatingsByCountry(country, page, size, search, sortBy, direction, onlyActive));
        return ResponseEntity.ok(liveRatingCacheService.findStdRatings(page, size, search, sortBy, direction, onlyActive));
    }

    private SortBy validateSort(String sort) {
        try {
            return SortBy.valueOf(sort);
        } catch (Exception _) {
            return SortBy.RATING;
        }
    }

    private SortDirection validateSortDirection(String dir) {
        try {
            return SortDirection.valueOf(dir);
        } catch (Exception _) {
            return SortDirection.DESC;
        }
    }

    @GetMapping("/rapid-ratings")
    public ResponseEntity<TopRatingResponseDTO> getRapidRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "ALL") String country,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "rating") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "true") boolean onlyActive
    ) {
        SortBy sortBy = validateSort(sort);
        SortDirection direction = validateSortDirection(dir);
        if(!Objects.equals(country, "ALL"))
            return ResponseEntity.ok(liveRatingCacheService.findRapidRatingsByCountry(country, page, size, search, sortBy, direction, onlyActive));
        return ResponseEntity.ok(liveRatingCacheService.findRapidRatings(page, size, search, sortBy, direction, onlyActive));
    }

    @GetMapping("/blitz-ratings")
    public ResponseEntity<TopRatingResponseDTO> getBlitzRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "ALL") String country,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "rating") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "true") boolean onlyActive) {
        SortBy sortBy = validateSort(sort);
        SortDirection direction = validateSortDirection(dir);
        if(!Objects.equals(country, "ALL"))
            return ResponseEntity.ok(liveRatingCacheService.findBlitzRatingsByCountry(country, page, size, search, sortBy, direction, onlyActive));
        return ResponseEntity.ok(liveRatingCacheService.findBlitzRatings(page, size, search, sortBy, direction, onlyActive));
    }
}
