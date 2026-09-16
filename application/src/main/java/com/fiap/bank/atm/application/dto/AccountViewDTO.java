package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountViewDTO(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String formattedBalance,
        BigDecimal remainingDailyLimit,
        String formattedRemainingDailyLimit,
        Boolean blocked) {
}
