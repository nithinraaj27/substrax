package com.substrax.paymentorchestrator.idempotency;


import com.substrax.common.exception.IdempotencyConflictException;
import com.substrax.paymentorchestrator.dto.IdempotencyDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);

    @Qualifier("idempotencyTemplate")
    private final RedisTemplate<String, IdempotencyRecord> redisTemplate;


    @Override
    public IdempotencyDecision validateAndRegister(String idempotencyKey, String requestHash) {
        IdempotencyRecord existing =
                redisTemplate.opsForValue().get(idempotencyKey);

        // FIRST REQUEST
        if (existing == null) {
            String transactionId = UUID.randomUUID().toString();

            redisTemplate.opsForValue().set(
                    idempotencyKey,
                    IdempotencyRecord.inProgress(transactionId, requestHash),
                    TTL
            );

            return new IdempotencyDecision(
                    true,
                    transactionId,
                    null,
                    null
            );
        }

        // SAME KEY, DIFFERENT PAYLOAD
        if (existing.requestHash() == null ||
                !existing.requestHash().equals(requestHash)) {

            throw new IdempotencyConflictException(
                    "Idempotency key reused with different request payload"
            );
        }

        // 🔁 REPLAY
        if (existing.isCompleted()) {
            return new IdempotencyDecision(
                    false,
                    existing.transactionId(),
                    existing.message(),
                    200
            );
        }

        // ⏳ IN PROGRESS
        throw new IdempotencyConflictException(
                "Request already in progress for this idempotency key"
        );
    }

    @Override
    public void markCompleted(String idempotencyKey, String status, String message) {
        IdempotencyRecord existing =
                redisTemplate.opsForValue().get(idempotencyKey);

        if (existing == null) return;

        redisTemplate.opsForValue().set(
                idempotencyKey,
                new IdempotencyRecord(
                        existing.transactionId(),
                        status,
                        message,
                        existing.requestHash(),
                        IdempotencyState.COMPLETED
                ),
                TTL
        );
    }


    @Override
    public void markFailed(String idempotencyKey) {
        redisTemplate.delete(idempotencyKey);
    }
}