package com.example.demo;

import com.example.demo.entity.LiveRating;
import com.example.demo.repository.LiveRatingRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableCaching
public class DemoApplication {
    public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
