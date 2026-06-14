package com.example.demo.batch;

import com.example.demo.dto.PlayerRatingHistoryDTO;
import com.example.demo.dto.RatingHistoryDTO;
import com.example.demo.entity.PeakRating;
import com.example.demo.entity.Player;
import com.example.demo.repository.PeakRatingRepository;
import com.example.demo.repository.PlayerRepository;
import com.example.demo.utils.TimeControl;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Configuration
public class WritePeakRatingJob {
    private static final Logger logger = LoggerFactory.getLogger(WritePeakRatingJob.class);

    @Bean
    public Job writePeakRating(JobRepository jobRepository, Step writePeakRatingStep){
        return new JobBuilder("writePeakRatingJob", jobRepository)
                .start(writePeakRatingStep)
                .build();
    }

    @Bean
    public Step writePeakRatingStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             @Qualifier("writePeakRatingReader") ItemReader<Player> reader,
                             @Qualifier("writePeakRatingProcessor") ItemProcessor<Player, PlayerRatingHistoryDTO> processor,
                             @Qualifier("writePeakRatingWriter") ItemWriter<PlayerRatingHistoryDTO> writer){
        return new StepBuilder("writePeakRatingStep", jobRepository)
                .<Player, PlayerRatingHistoryDTO>chunk(500)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(1000)
                .listener(new ItemReadListener<Player>() {
                    @Override
                    public void onReadError(@NonNull Exception ex) {
                        logger.error("Error reading player: {}", ex.getMessage());
                    }
                })
                .build();
    }

    @Bean("writePeakRatingReader")
    public RepositoryItemReader<Player> writePeakRatingReader(PlayerRepository playerRepository){
        return new RepositoryItemReaderBuilder<Player>()
                .name("writePeakRatingReader")
                .repository(playerRepository)
                .methodName("findAll")
                .pageSize(500)
                .sorts(Collections.singletonMap("fideId", Sort.Direction.ASC))
                .build();
    }

    @Bean("writePeakRatingProcessor")
    public ItemProcessor<Player, PlayerRatingHistoryDTO> writePeakRatingProcessor(WebClient lichessClient) {
        return player -> {
            try {
                RatingHistoryDTO historyDTO =  lichessClient.get()
                        .uri("/api/fide/player/"+ player.getFideId()+"/ratings")
                        .retrieve()
                        .bodyToMono(RatingHistoryDTO.class)
                        .block();
                return new PlayerRatingHistoryDTO(player, historyDTO);
            } catch (Exception e) {
                logger.error("Error fetching rating history for player {}: {}", player.getName(), e.getMessage());
                return null;
            }
        };
    }

    @Bean("writePeakRatingWriter")
    public ItemWriter<PlayerRatingHistoryDTO> writePeakRatingWriter(PeakRatingRepository peakRatingRepository){
        return playerRatingHistories -> {
            List<PeakRating> peakRatings = new ArrayList<>(playerRatingHistories.size() * 3);
            for(PlayerRatingHistoryDTO dto : playerRatingHistories) {
                if(dto.getRatingHistory() == null) continue;
                RatingHistoryDTO historyDTO = dto.getRatingHistory();
                if(isValidHistory(historyDTO.getStandard())) {
                    peakRatings.add(findPeak(historyDTO.getStandard(), TimeControl.STD, dto.getPlayer()));
                }
                if(isValidHistory(historyDTO.getRapid())){
                    peakRatings.add(findPeak(historyDTO.getRapid(), TimeControl.RAPID, dto.getPlayer()));
                }
                if(isValidHistory(historyDTO.getBlitz())){
                    peakRatings.add(findPeak(historyDTO.getBlitz(), TimeControl.BLITZ, dto.getPlayer()));
                }
        }
            peakRatingRepository.saveAll(peakRatings);
        };
    }

    private boolean isValidHistory(List<Long> history){
        return history != null && !history.isEmpty();
    }

    private PeakRating findPeak(List<Long> history, TimeControl timeControl, Player player){
        PeakRating peakRating = new PeakRating();
        peakRating.setTimeControl(timeControl);
        short peak = 0;
        LocalDate peakDate = null;
        for(Long data : history) {
            short rating = (short) (data % 10_000);
            if(rating > peak) {
                peak = rating;
                int year = (int) (data / 1_000_000);
                int month = (int) ((data / 10_000) % 100);
                peakDate = LocalDate.of(year, month, 1);
            }
        }
        peakRating.setRating(peak);
        peakRating.setRatingDate(peakDate);
        peakRating.setPlayer(player);
        return peakRating;
    }
}
