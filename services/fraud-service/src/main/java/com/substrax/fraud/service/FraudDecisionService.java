package com.substrax.fraud.service;

import com.substrax.fraud.dto.FraudResult;

import java.math.BigDecimal;
import java.util.UUID;

public interface FraudDecisionService {

    FraudResult evaluate(String transactionId, String userId, BigDecimal amount);
}
