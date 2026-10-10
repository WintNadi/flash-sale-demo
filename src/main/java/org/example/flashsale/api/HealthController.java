package org.example.flashsale.api;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final StringRedisTemplate redis;

    public HealthController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "flash-sale");
        result.put("timestamp", System.currentTimeMillis());

        try {
            String pong = redis.getConnectionFactory().getConnection().ping();
            result.put("redis", "PONG".equalsIgnoreCase(pong) ? "UP" : pong);
        } catch (Exception e) {
            result.put("redis", "DOWN: " + e.getMessage());
        }

        return result;
    }
}