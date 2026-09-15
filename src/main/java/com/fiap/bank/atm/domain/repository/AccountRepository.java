package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.Account;
import java.util.Optional;

// Interface

public interface AccountRepository {
    Optional<Account> findByAccountNumber(String accountNumber);
    void save(Account account);
}