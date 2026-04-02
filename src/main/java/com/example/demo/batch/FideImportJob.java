package com.example.demo.batch;

import com.example.demo.entity.Player;
import com.example.demo.entity.Rating;
import com.example.demo.repository.PlayerRepository;
import com.example.demo.repository.RatingRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemReader;
import org.springframework.batch.infrastructure.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class FideImportJob {

    @Bean
    public Job importPlayersJob(JobRepository jobRepository, Step importPlayersStep){
        return new JobBuilder("importPlayersJob", jobRepository)
                .start(importPlayersStep)
                .build();
    }

    @Bean
    Step importPlayersStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           ItemReader<Player> reader,
                           ItemProcessor<Player, Player> processor,
                           ItemWriter<Player> writer){
        return new StepBuilder("importPlayersStep", jobRepository)
                .<Player, Player>chunk(500)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .listener(new ItemReadListener<Player>() {
                    @Override
                    public void onReadError(@NonNull Exception ex) {
                        System.out.println("Error reading player: " + ex.getMessage());
                    }
                })
                .build();
    }

    @Bean
    StaxEventItemReader<Player> reader(){
        return new StaxEventItemReaderBuilder<Player>()
                .name("playerReader")
                .resource(new FileSystemResource("F:/players.xml"))
                .addFragmentRootElements("player")
                .unmarshaller(playerMarshaller())
                .build();
    }

    @Bean
    Jaxb2Marshaller playerMarshaller(){
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(Player.class);
        return marshaller;
    }

    @Bean
    ItemProcessor<Player, Player> processor(){
        return player -> {
            if(player.getStdRating() >= 2000) return player;
            return null;
        };
    }

    @Bean
    ItemWriter<Player> writer(PlayerRepository playerRepository, RatingRepository ratingRepository) {
        return players -> {
            playerRepository.saveAll(players);

            LocalDate period = LocalDate.now().withDayOfMonth(1);

            List<Rating> ratings = players.getItems().stream()
                    .map(player -> {
                        Rating rating = new Rating();
                        rating.setPlayer(player);
                        rating.setPeriod(period);
                        rating.setStdRating(player.getStdRating());
                        rating.setRapidRating(player.getRapidRating());
                        rating.setBlitzRating(player.getBlitzRating());
                        return rating;
                    }).toList();

            ratingRepository.saveAll(ratings);
        };
    }
}