package com.example.demo.Services;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;

@Service
public class RateLimitingService {

    private final long strictIpCapacity;
    private final long strictIpDurationMinutes;
    private final long generalIpCapacity;
    private final long generalIpDurationMinutes;
    private final long usernameCapacity;
    private final long usernameDurationMinutes;

    // Self-cleaning Caffeine caches: automatically evicts entries after idle timeout
    private final Cache<String, Bucket> strictIpCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    private final Cache<String, Bucket> generalIpCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(50_000)
            .build();

    private final Cache<String, Bucket> usernameCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(20))
            .maximumSize(20_000)
            .build();

    public RateLimitingService(
            @Value("${ratelimit.strict.ip.capacity:5}") long strictIpCapacity,
            @Value("${ratelimit.strict.ip.duration-minutes:1}") long strictIpDurationMinutes,
            @Value("${ratelimit.general.ip.capacity:100}") long generalIpCapacity,
            @Value("${ratelimit.general.ip.duration-minutes:1}") long generalIpDurationMinutes,
            @Value("${ratelimit.username.capacity:5}") long usernameCapacity,
            @Value("${ratelimit.username.duration-minutes:15}") long usernameDurationMinutes
    ) {
        this.strictIpCapacity = strictIpCapacity;
        this.strictIpDurationMinutes = strictIpDurationMinutes;
        this.generalIpCapacity = generalIpCapacity;
        this.generalIpDurationMinutes = generalIpDurationMinutes;
        this.usernameCapacity = usernameCapacity;
        this.usernameDurationMinutes = usernameDurationMinutes;
    }

    // --- Key 1: IP Rate Limiting ---

    public ConsumptionProbe probeConsumeStrictIp(String ip) {
        Bucket bucket = strictIpCache.get(ip, k -> createBucket(strictIpCapacity, strictIpDurationMinutes));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe probeConsumeGeneralIp(String ip) {
        Bucket bucket = generalIpCache.get(ip, k -> createBucket(generalIpCapacity, generalIpDurationMinutes));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    // --- Key 2: Username Rate Limiting (Stops Rotating-IP attacks) ---

    public EstimationProbe checkUsernameAllowed(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        Bucket bucket = usernameCache.getIfPresent(username.toLowerCase());
        if (bucket == null) {
            return null; // No failed attempts recorded yet
        }
        EstimationProbe probe = bucket.estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            return probe; // Blocked: contains nanosToWaitForRefill
        }
        return null; // Allowed
    }

    public ConsumptionProbe recordFailedLogin(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        Bucket bucket = usernameCache.get(username.toLowerCase(),
                k -> createBucket(usernameCapacity, usernameDurationMinutes));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public void resetUsernameLimit(String username) {
        if (username != null && !username.isBlank()) {
            usernameCache.invalidate(username.toLowerCase());
        }
    }

    private Bucket createBucket(long capacity, long durationMinutes) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(durationMinutes))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
