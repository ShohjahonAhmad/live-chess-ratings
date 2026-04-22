package com.example.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.Semaphore;

/**
 * Global rate limiter to enforce a maximum of 4 concurrent API requests
 * across the entire application (discovery, cleanup, and game streams).
 * 
 * This prevents exceeding Lichess API rate limits by ensuring that all
 * HTTP requests (from BroadcastDiscoveryWorker, LiveGameStreamService, etc.)
 * collectively never exceed the configured limit.
 */
@Component
public class GlobalRateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(GlobalRateLimiter.class);
    
    // Semaphore with 4 permits - max concurrent API requests
    private final Semaphore semaphore = new Semaphore(4);
    private final int maxConcurrentRequests = 4;

    /**
     * Wraps a Mono request with semaphore acquire/release logic.
     * Ensures the request only executes when a permit is available.
     * 
     * @param request The Mono representing the HTTP request
     * @param <T> The type of the response
     * @return A Mono that will acquire a permit, execute the request, and release the permit
     */
    public <T> Mono<T> executeWithRateLimit(Mono<T> request, String requestName) {
        return Mono.fromRunnable(() -> {
            try {
                int remainingPermits = semaphore.availablePermits();
                logger.debug("Rate limiter: acquiring permit for [{}]. Available permits: {}/{}", 
                    requestName, remainingPermits, maxConcurrentRequests);
                semaphore.acquire();
                logger.debug("Rate limiter: permit acquired for [{}]. Remaining permits: {}/{}", 
                    requestName, semaphore.availablePermits(), maxConcurrentRequests);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while acquiring rate limiter permit", e);
            }
        })
        .then(request)
        .doFinally(signal -> {
            semaphore.release();
            logger.debug("Rate limiter: permit released. Available permits: {}/{}", 
                semaphore.availablePermits(), maxConcurrentRequests);
        });
    }

    /**
     * Get the current number of available permits
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * Get the maximum concurrent requests allowed
     */
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }
}

