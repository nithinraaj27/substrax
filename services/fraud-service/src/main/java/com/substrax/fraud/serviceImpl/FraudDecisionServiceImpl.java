package com.substrax.fraud.serviceImpl;

import com.substrax.fraud.dto.FraudDecision;
import com.substrax.fraud.dto.FraudResult;
import com.substrax.fraud.service.FraudDecisionService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDecisionServiceImpl implements FraudDecisionService {

    private static final BigDecimal HARD_LIMIT = BigDecimal.valueOf(50_000);
    private static final int VELOCITY_LIMIT = 5;
    private static final Duration VELOCITY_WINDOW = Duration.ofMinutes(10);
    private static final Duration TX_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    @Retry(name="fraudService", fallbackMethod = "fraudFallback")
    @CircuitBreaker(name="fraudService", fallbackMethod = "fraudFallback")
    @Override
    public FraudResult evaluate(String transactionId, String userId, BigDecimal amount) {

        // 1 Duplicate transaction check
        String txKey = "fraud:tx"+transactionId;
        if(Boolean.TRUE.equals(redisTemplate.hasKey(txKey)))
        {
            return reject(transactionId, "DUPLICATE_TRANSACTION");
        }

        // 2 Velocity Check
        String userKey = "fraud:user:" + userId;
        Long count = redisTemplate.opsForValue().increment(userKey);

        if(count != null && count == 1)
        {
            redisTemplate.expire(userKey, VELOCITY_WINDOW);
        }

        if(count != null && count > VELOCITY_LIMIT)
        {
            return reject(transactionId, "HIGH_VELOCITY");
        }

        // 3 Amount Threshold
        if(amount.compareTo(HARD_LIMIT) > 0)
        {
            return reject(transactionId, "AMOUNT_THRESHOLD_EXCEEDED");
        }

        // 4 Approve
        redisTemplate.opsForValue().set(txKey, "PROCESSED", TX_TTL);

        FraudResult result = new FraudResult(transactionId, FraudDecision.APPROVED, "CLEAN" );
        log.info(
                "Fraud decision={} txId={} userId={} amount={}",
                result.decision(), transactionId, userId, amount
        );
        return result;
    }

    private FraudResult reject(String transactionId, String reason)
    {

        redisTemplate.opsForValue()
                .set("fraud:tx:" + transactionId, "REJECTED", TX_TTL);

        return new FraudResult(transactionId, FraudDecision.REJECTED, reason);

    }

    public FraudResult fraudFallback(
            String transactionId,
            String userId,
            BigDecimal amount,
            Exception ex
    ) {
        log.error("Fraud retry exhausted / circuit open txId={}", transactionId, ex);

        // IMPORTANT: do NOT approve or reject
        // Let orchestrator timeout logic handle it
        return new FraudResult(
                transactionId,
                FraudDecision.PENDING,
                "FRAUD_SERVICE_UNAVAILABLE"
        );
    }
}
