package com.substrax.paymentorchestrator.idempotency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class RedisIdempotencyService implements IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private final RedisTemplate<String, IdempotencyRecord> redisTemplate;

    public RedisIdempotencyService(@Qualifier("idempotencyTemplate") RedisTemplate<String, IdempotencyRecord> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(String key) {
        return "Payment:idempotency:" + key;
    }

    @Override
    public Optional<IdempotencyRecord> get(String key) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(key)));
        } catch (Exception e) {
            log.error("Redis GET failure for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(String key, IdempotencyRecord record) {
        try {
            redisTemplate.opsForValue().set(buildKey(key), record, TTL);
        } catch (Exception e) {
            log.error("Redis SAVE failure for key {}: {}", key, e.getMessage());
        }
    }
}