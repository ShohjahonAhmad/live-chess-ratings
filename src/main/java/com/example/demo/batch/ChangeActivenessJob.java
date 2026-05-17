package com.example.demo.batch;

import com.example.demo.dto.PlayerActivenessDTO;
import com.example.demo.entity.Player;
import com.example.demo.repository.PlayerRepository;
import com.example.demo.utils.TimeControl;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemReader;
import org.springframework.batch.infrastructure.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.MalformedURLException;
import java.util.*;


@Configuration
public class ChangeActivenessJob {

    private static final Logger logger = LoggerFactory.getLogger(ChangeActivenessJob.class);

    @Bean
    public Job changeActiveness(JobRepository jobRepository, Step changeActivenessStep) {
        return new JobBuilder("changeActiveness", jobRepository)
                .start(changeActivenessStep)
                .build();
    }

    @Bean
    Step changeActivenessStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              @Qualifier("changeActivenessReader") ItemReader<PlayerActivenessDTO> reader,
                              @Qualifier("changeActivenessWriter")ItemWriter<PlayerActivenessDTO> writer) {
        return new StepBuilder("changeActivenessStep", jobRepository)
                .<PlayerActivenessDTO, PlayerActivenessDTO>chunk(500)
                .transactionManager(transactionManager)
                .reader(reader)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .listener(new ItemReadListener<PlayerActivenessDTO>() {
                    @Override
                    public void onReadError(@NonNull Exception ex){
                        logger.error("Error reading player: {}", ex.getMessage());
                    }
                })
                .build();
    }

    @Bean("changeActivenessReader")
    @StepScope
    StaxEventItemReader<PlayerActivenessDTO> reader(@Value("#{jobParameters['fileUrl']}") String fileUrl) {
        if(fileUrl == null) throw new IllegalArgumentException("fileUrl parameter is required");
        try {
            UrlResource urlResource = new UrlResource(fileUrl);
            return new StaxEventItemReaderBuilder<PlayerActivenessDTO>()
                    .name("playerActivenessReader")
                    .resource(urlResource)
                    .addFragmentRootElements("player")
                    .unmarshaller(playerMarshaller())
                    .build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file url: " + fileUrl, e);
        }
    }

    @Bean("changeActivenessMarshaller")
    Jaxb2Marshaller playerMarshaller(){
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(PlayerActivenessDTO.class);
        return marshaller;
    }

    @Bean("changeActivenessWriter")
    @StepScope
    ItemWriter<PlayerActivenessDTO> writer(@Value("#{jobParameters['timeControl']}") String timeControl,
                                           PlayerRepository playerRepository){
        return players -> {
            TimeControl timeControlEnum = TimeControl.valueOf(timeControl);
            List<Long> ids = new ArrayList<>();
            for(PlayerActivenessDTO playerActivenessDTO : players) {
                ids.add(playerActivenessDTO.getFideId());
            }

            Map<Long, Player> playerMap = new HashMap<>();
            for(Player player : playerRepository.findAllById(ids)){
                playerMap.put(player.getFideId(), player);
            }

            List<Player> toSave = new ArrayList<>();
            for(PlayerActivenessDTO playerActivenessDTO : players) {
                Player player = playerMap.get(playerActivenessDTO.getFideId());
                if(player == null) continue;
                Player updated = setFlag(playerActivenessDTO.getFlag(), player, timeControlEnum);
                if (updated != null) toSave.add(updated);
            }

            playerRepository.saveAll(toSave);
        };
    }

    private Player setFlag(String flag, Player player, TimeControl timeControl) {
        switch (timeControl) {
            case STD -> {
                if (Objects.equals(player.getFlag(), flag)) return null;
                player.setFlag(flag);
            }
            case RAPID -> {
                if (Objects.equals(player.getRapidFlag(), flag)) return null;
                player.setRapidFlag(flag);
            }
            case BLITZ -> {
                if (Objects.equals(player.getBlitzFlag(), flag)) return null;
                player.setBlitzFlag(flag);
            }
            default -> logger.warn("Unknown time control: {}", timeControl);
        }
        return player;
    }
}
