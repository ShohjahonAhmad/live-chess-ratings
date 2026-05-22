package com.example.demo.service;

import com.example.demo.dto.TopRatingDTO;
import com.example.demo.dto.FullTopRatingDTO;
import com.example.demo.dto.TopRatingResponseDTO;
import com.example.demo.repository.LiveRatingRepository;
import com.example.demo.utils.TimeControl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LiveRatingCacheService {
    private static final Logger logger = LoggerFactory.getLogger(LiveRatingCacheService.class);

    private final LiveRatingRepository liveRatingRepository;
    private final ObjectMapper objectMapper;

    private List<FullTopRatingDTO> cachedStd = new ArrayList<>();
    private List<FullTopRatingDTO> cachedRapid = new ArrayList<>();
    private List<FullTopRatingDTO> cachedBlitz = new ArrayList<>();

    public LiveRatingCacheService(LiveRatingRepository liveRatingRepository, ObjectMapper objectMapper) {
        this.liveRatingRepository = liveRatingRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    @Scheduled(fixedDelay = 300000)
    public void refreshAllCaches() {
        logger.info("Refreshing in-memory ratings cache...");
        cachedStd = fetchAll(TimeControl.STD);
        cachedRapid = fetchAll(TimeControl.RAPID);
        cachedBlitz = fetchAll(TimeControl.BLITZ);
    }

    public List<FullTopRatingDTO> fetchAll(TimeControl timeControl){
        List<TopRatingDTO> result = switch (timeControl) {
            case STD -> liveRatingRepository.findStdPlayers();
            case RAPID -> liveRatingRepository.findRapidPLayers();
            case BLITZ -> liveRatingRepository.findBlitzPLayers();
        };

        return result.stream().map(dto -> {
            try {
                return FullTopRatingDTO.from(dto, objectMapper);
            } catch (JsonProcessingException e){
                throw new RuntimeException(e);
            }
        }).toList();
    }


    public TopRatingResponseDTO findStdRatings(int page, int size) {
        int total = cachedStd.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);

        return new TopRatingResponseDTO(cachedStd.subList(from, to), total);
    }

    public TopRatingResponseDTO findRapidRatings(int page, int size) {
        int total = cachedRapid.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);

        return new TopRatingResponseDTO(cachedRapid.subList(from, to), total);
    }

    public TopRatingResponseDTO findBlitzRatings(int page, int size) {
        int total = cachedBlitz.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);

        return new TopRatingResponseDTO(cachedBlitz.subList(from, to), total);
    }
}
