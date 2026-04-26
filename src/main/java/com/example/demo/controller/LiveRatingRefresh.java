package com.example.demo.controller;

import com.example.demo.dto.MonthlyRatingsResponseDTO;
import com.example.demo.service.LiveRatingRefreshService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class LiveRatingRefresh {

    private final LiveRatingRefreshService liveRatingRefreshService;

    public LiveRatingRefresh(LiveRatingRefreshService liveRatingRefreshService) {
        this.liveRatingRefreshService = liveRatingRefreshService;
    }

    @PostMapping("/refresh-live-ratings")
    public ResponseEntity<MonthlyRatingsResponseDTO> refreshLiveRatings(@RequestParam(required = false) String importDate) {
        try {
            LocalDate period = LocalDate.now();
            try {
                period = LocalDate.parse(importDate);
            } catch (Exception e) {}
            period = period.withDayOfMonth(1);
            liveRatingRefreshService.refreshLiveRatings(period);
            return ResponseEntity.ok(new MonthlyRatingsResponseDTO(true, "Live ratings refreshed successfully for period: " + period));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MonthlyRatingsResponseDTO(false, e.getMessage()));
        }
    }
}
