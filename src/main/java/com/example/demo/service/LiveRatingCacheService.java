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


    public TopRatingResponseDTO findStdRatings(int page, int size, String nameOrId) {
        return findBySearch(cachedStd, page, size, nameOrId);
    }

    public TopRatingResponseDTO findRapidRatings(int page, int size, String nameOrId) {
        return findBySearch(cachedRapid, page, size, nameOrId);
    }

    public TopRatingResponseDTO findBlitzRatings(int page, int size, String nameOrId) {
        return findBySearch(cachedBlitz, page, size, nameOrId);
    }

    public TopRatingResponseDTO findBySearch(List<FullTopRatingDTO> cache, int page, int size, String nameOrId){
        List<FullTopRatingDTO> content = new ArrayList<>();

        if(nameOrId != null && !nameOrId.isBlank()){
            int rank = 1;
            for(FullTopRatingDTO fullTopRatingDTO : cache) {
                if(isSimilar(nameOrId, fullTopRatingDTO.getName(), String.valueOf(fullTopRatingDTO.getFideId()))){
                    FullTopRatingDTO copy = new FullTopRatingDTO(fullTopRatingDTO);
                    copy.setRank((long) rank++);
                    content.add(copy);
                }
            }
        } else {
            content = cache;
        }

        int total = content.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);

        return new TopRatingResponseDTO(from >= total ? List.of() : content.subList(from, to), content.size());
    }

    public TopRatingResponseDTO findStdRatingsByCountry(String country, int page, int size, String nameOrFideId) {
        return getRatingsByCountry(cachedStd, country, page, size, nameOrFideId);
    }

    public TopRatingResponseDTO findRapidRatingsByCountry(String country, int page, int size, String nameOrFideId) {
        return getRatingsByCountry(cachedRapid, country, page, size, nameOrFideId);
    }

    public TopRatingResponseDTO findBlitzRatingsByCountry(String country, int page, int size, String nameOrFideId) {
        return getRatingsByCountry(cachedBlitz, country, page, size, nameOrFideId);
    }

    private TopRatingResponseDTO getRatingsByCountry(List<FullTopRatingDTO> cache, String country, int page, int size, String nameOrFideId){
        List<FullTopRatingDTO> filtered = new ArrayList<>();
        int rank = 1;
        for(FullTopRatingDTO dto : cache){
            if(dto.getCountry().equalsIgnoreCase(country)){
                if(nameOrFideId.isEmpty()){
                    FullTopRatingDTO copy = new FullTopRatingDTO(dto);
                    copy.setRank((long) rank++);
                    filtered.add(copy);
                } else {
                    if(isSimilar(nameOrFideId, dto.getName(), String.valueOf(dto.getFideId()))){
                        FullTopRatingDTO copy = new FullTopRatingDTO(dto);
                        copy.setRank((long) rank++);
                        filtered.add(copy);
                    }
                }
            }
        }

        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);

        return new TopRatingResponseDTO(from >= total ? List.of() : filtered.subList(from, to), filtered.size());
    }

    private boolean isSimilar(String nameOrFideId, String name, String fideID){
        return formatName(name).toLowerCase().contains(nameOrFideId.toLowerCase()) || fideID.contains(nameOrFideId);
    }

    private String formatName(String fullName) {
        String[] parts = fullName.split(",");

        if(parts.length == 2) {
            return (parts[1].trim() + " " + parts[0].trim())
                    .toLowerCase();
        }

        return fullName.trim().toLowerCase();
    }
}
