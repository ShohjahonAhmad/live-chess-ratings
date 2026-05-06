package com.example.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CacheConfiguration {

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES);
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);

        caffeineCacheManager.registerCustomCache(
                "std",
                Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build());

        caffeineCacheManager.registerCustomCache(
                "rapid",
                Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build());

        caffeineCacheManager.registerCustomCache(
                "blitz",
                Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build());

        return caffeineCacheManager;
    }
}
