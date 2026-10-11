package org.example.flashsale.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.flashsale.api.dto.ReservationDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
public class ReservationService {

    public static final String KEY_PREFIX = "reservation:";
    public static final Duration RESERVATION_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ReservationService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * Store a reservation in Redis with a TTL.
     * Returns true if successfully stored, false if Redis failed.
     */
    public boolean hold(ReservationDto reservation) {
        String key = KEY_PREFIX + reservation.orderId();
        try {
            String value = objectMapper.writeValueAsString(reservation);
            redis.opsForValue().set(key, value, RESERVATION_TTL);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to store reservation in Redis: " + e.getMessage());
            return false;
        }
    }

    /** Read a reservation from Redis, if it hasn't expired. */
    public Optional<ReservationDto> find(long orderId) {
        String key = KEY_PREFIX + orderId;
        try {
            String value = redis.opsForValue().get(key);
            if (value == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(value, ReservationDto.class));
        } catch (Exception e) {
            System.err.println("Failed to read reservation from Redis: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Release a reservation early (confirm, cancel, or expiry). */
    public void release(long orderId) {
        redis.delete(KEY_PREFIX + orderId);
    }

    /** Remaining TTL in seconds. -1 if key has no TTL, -2 if key doesn't exist. */
    public long ttlSeconds(long orderId) {
        Long ttl = redis.getExpire(KEY_PREFIX + orderId);
        return ttl == null ? -1 : ttl;
    }

    /** All active reservation keys currently in Redis. */
    public Set<String> findAllActiveKeys() {
        return redis.keys(KEY_PREFIX + "*");
    }
}