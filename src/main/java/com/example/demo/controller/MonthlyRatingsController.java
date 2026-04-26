package com.example.demo.controller;

import com.example.demo.dto.MonthlyRatingsRequestDTO;
import com.example.demo.dto.MonthlyRatingsResponseDTO;
import com.example.demo.service.MonthlyRatingsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonthlyRatingsController {
    private final MonthlyRatingsService monthlyRatingsService;
    private static final Logger logger = LoggerFactory.getLogger(MonthlyRatingsController.class);

    public MonthlyRatingsController(MonthlyRatingsService monthlyRatingsService) {
        this.monthlyRatingsService = monthlyRatingsService;
    }

    @PostMapping("/monthly-ratings")
    public ResponseEntity<MonthlyRatingsResponseDTO> postMonthlyRatings(@Valid @RequestBody MonthlyRatingsRequestDTO requestDTO) {
       try {
           MonthlyRatingsResponseDTO responseDTO = monthlyRatingsService.importMonthlyRatings(requestDTO.getLinkToFile(), requestDTO.getDate());
           logger.debug("Import monthly ratings response: success={}, message={}", responseDTO.isSuccess(), responseDTO.getMessage());
           if(responseDTO.success) {
               return ResponseEntity.ok(responseDTO);
           } else {
               return ResponseEntity.badRequest().body(responseDTO);
           }
       } catch (Exception e) {
           logger.error("Error importing monthly ratings: {}", e.getMessage());
           return ResponseEntity.status(500).body(new MonthlyRatingsResponseDTO(false, e.getMessage()));
       }
    }
}
