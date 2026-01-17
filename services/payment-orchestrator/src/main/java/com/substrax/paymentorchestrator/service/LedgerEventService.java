package com.substrax.paymentorchestrator.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerEventService {

    void emitLedgerDebit(UUID eventId, String transactionId, String userLedgerId, double amount, String currency, String reference);

    void emitLedgerCredit(UUID eventId, String transactionId, double amount, String currency, String reference);
}
