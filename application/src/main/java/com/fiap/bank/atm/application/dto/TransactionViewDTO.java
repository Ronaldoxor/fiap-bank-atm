package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionViewDTO(
        UUID id,
        LocalDateTime timestamp,
        String type,
        String typeDescription,
        BigDecimal amount,
        String formattedAmount,
        String description) {
}
