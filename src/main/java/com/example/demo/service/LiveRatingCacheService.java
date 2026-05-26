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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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


    public TopRatingResponseDTO findStdRatings(int page, int size, String nameOrId, String sortBy, String dir) {
        return findBySearch(cachedStd, page, size, nameOrId, sortBy, dir);
    }

    public TopRatingResponseDTO findRapidRatings(int page, int size, String nameOrId, String sortBy, String dir) {
        return findBySearch(cachedRapid, page, size, nameOrId, sortBy, dir);
    }

    public TopRatingResponseDTO findBlitzRatings(int page, int size, String nameOrId, String sortBy, String dir) {
        return findBySearch(cachedBlitz, page, size, nameOrId, sortBy, dir);
    }

    public TopRatingResponseDTO findBySearch(List<FullTopRatingDTO> cache, int page, int size, String nameOrId, String sortBy, String dir){
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
            content = new ArrayList<>(cache);
        }

        if(!(Objects.equals(sortBy, "rating") && Objects.equals(dir, "desc"))){
            content.sort((a, b) -> {
                if(Objects.equals(sortBy, "rating")){
                    double ratingA = a.getRating() == null ? 0 : a.getRating();
                    double ratingB = b.getRating() == null ? 0 : b.getRating();
                    if(Objects.equals(dir, "asc")){
                        return Double.compare(ratingA, ratingB);
                    }
                } else if(Objects.equals(sortBy, "ratingChange")){
                    double ratingChangeA = a.getRatingChange() == null ? 0 : a.getRatingChange();
                    double ratingChangeB = b.getRatingChange() == null ? 0 : b.getRatingChange();
                    if(Objects.equals(dir, "asc")){
                        return Double.compare(ratingChangeA, ratingChangeB);
                    } else {
                        return Double.compare(ratingChangeB, ratingChangeA);
                    }
                } else if (Objects.equals(sortBy, "year")) {
                    short yearA = a.getYear();
                    short yearB = b.getYear();
                    if(yearA == 0 && yearB == 0) return 0;
                    if(yearA == 0) return 1;
                    if(yearB == 0) return -1;

                    if(Objects.equals(dir, "asc")){
                        return Short.compare(yearA, yearB);
                    } else {
                        return Short.compare(yearB, yearA);
                    }
                } else if (Objects.equals(sortBy, "count")) {
                    long countA = a.getCount() == null ? 0 : a.getCount();
                    long countB = b.getCount() == null ? 0 : b.getCount();
                    if(Objects.equals(dir, "asc")){
                        return Long.compare(countA, countB);
                    } else {
                        return Long.compare(countB, countA);
                    }
                }
                return 0;
            });
        }


        int total = content.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);

        return new TopRatingResponseDTO(from >= total ? List.of() : content.subList(from, to), content.size());
    }

    public TopRatingResponseDTO findStdRatingsByCountry(String country, int page, int size, String nameOrFideId, String sortBy, String dir) {
        return getRatingsByCountry(cachedStd, country, page, size, nameOrFideId, sortBy, dir);
    }

    public TopRatingResponseDTO findRapidRatingsByCountry(String country, int page, int size, String nameOrFideId, String sortBy, String dir) {
        return getRatingsByCountry(cachedRapid, country, page, size, nameOrFideId, sortBy, dir);
    }

    public TopRatingResponseDTO findBlitzRatingsByCountry(String country, int page, int size, String nameOrFideId, String sortBy, String dir) {
        return getRatingsByCountry(cachedBlitz, country, page, size, nameOrFideId, sortBy, dir);
    }

    private TopRatingResponseDTO getRatingsByCountry(List<FullTopRatingDTO> cache, String country, int page, int size, String nameOrFideId, String sortBy, String dir){
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

        Comparator<Short> comparator =
                Comparator.nullsLast(Short::compareTo);

        if(!(Objects.equals(sortBy, "rating") && Objects.equals(dir, "desc"))){
            filtered.sort((a, b) -> {
                if(Objects.equals(sortBy, "rating")){
                    double ratingA = a.getRating() == null ? 0 : a.getRating();
                    double ratingB = b.getRating() == null ? 0 : b.getRating();
                    if(Objects.equals(dir, "asc")){
                        return Double.compare(ratingA, ratingB);
                    }
                } else if(Objects.equals(sortBy, "ratingChange")){
                    double ratingChangeA = a.getRatingChange() == null ? 0 : a.getRatingChange();
                    double ratingChangeB = b.getRatingChange() == null ? 0 : b.getRatingChange();
                    if(Objects.equals(dir, "asc")){
                        return Double.compare(ratingChangeA, ratingChangeB);
                    } else {
                        return Double.compare(ratingChangeB, ratingChangeA);
                    }
                } else if (Objects.equals(sortBy, "year")) {
                    short yearA = a.getYear();
                    short yearB = b.getYear();

                    if(yearA == 0 && yearB == 0) return 0;
                    if(yearA == 0) return 1;
                    if(yearB == 0) return -1;

                    if(Objects.equals(dir, "asc")){
                        return Short.compare(yearA, yearB);
                    } else {
                        return Short.compare(yearB, yearA);
                    }
                } else if (Objects.equals(sortBy, "count")) {
                    long countA = a.getCount() == null ? 0 : a.getCount();
                    long countB = b.getCount() == null ? 0 : b.getCount();
                    if(Objects.equals(dir, "asc")){
                        return Long.compare(countA, countB);
                    } else {
                        return Long.compare(countB, countA);
                    }
                }
                return 0;
            });
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
