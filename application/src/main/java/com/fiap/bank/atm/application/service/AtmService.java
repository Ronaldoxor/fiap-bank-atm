package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.application.dto.AccountViewDTO;
import com.fiap.bank.atm.application.dto.TransactionViewDTO;
import com.fiap.bank.atm.application.exception.AtmOperationException;
import com.fiap.bank.atm.application.exception.AtmOperationException.Reason;
import com.fiap.bank.atm.domain.exception.AccountBlockedException;
import com.fiap.bank.atm.domain.exception.DailyLimitExceededException;
import com.fiap.bank.atm.domain.exception.InsufficientFundsException;
import com.fiap.bank.atm.domain.exception.InvalidPinException;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class AtmService {
    private final AccountRepository accountRepository;
    private Account currentAccount;

    public AtmService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountViewDTO authenticate(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AtmOperationException(Reason.INVALID_PIN, "Conta não encontrada."));

        try {
            account.authenticate(pin);
            accountRepository.salvar(account); // Grava o zeramento das tentativas
            currentAccount = account;
            return toAccountView(account);
        } catch (RuntimeException e) {
            accountRepository.salvar(account); // Grava as tentativas erradas e o bloqueio
            throw translate(e);
        }
    }

    public void withdraw(BigDecimal amount) {
        ensureAuthenticated();
        applyBusinessRule(() -> currentAccount.withdraw(Money.of(amount)));
        accountRepository.salvar(currentAccount);
    }

    public void deposit(BigDecimal amount) {
        ensureAuthenticated();
        applyBusinessRule(() -> currentAccount.deposit(Money.of(amount)));
        accountRepository.salvar(currentAccount);
    }

    public void transfer(String targetAccountNumber, BigDecimal amount) {
        ensureAuthenticated();

        Account targetAccount = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new AtmOperationException(Reason.INVALID_OPERATION, "Conta de destino não encontrada."));
        applyBusinessRule(() -> currentAccount.transfer(targetAccount, Money.of(amount)));

        accountRepository.salvar(currentAccount);
        accountRepository.salvar(targetAccount);
    }

    public BigDecimal getBalance() {
        ensureAuthenticated();
        return currentAccount.getBalance().getAmount();
    }

    // Mais recente primeiro
    public List<TransactionViewDTO> getStatement() {
        ensureAuthenticated();
        return currentAccount.getTransactions().reversed().stream()
                .map(AtmService::toTransactionView)
                .toList();
    }

    public void logout() {
        currentAccount = null;
    }

    public Optional<AccountViewDTO> getCurrentAccount() {
        return Optional.ofNullable(currentAccount)
                .map(AtmService::toAccountView);
    }

    public Boolean isAuthenticated() {
        return currentAccount != null;
    }

    private void ensureAuthenticated() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário está autenticado no momento.");
        }
    }

    private void applyBusinessRule(Runnable businessRule) {
        try {
            businessRule.run();
        } catch (RuntimeException e) {
            throw translate(e);
        }
    }

    // Converte as exceções do domínio para a camada de apresentação
    private static RuntimeException translate(RuntimeException error) {
        if (error instanceof AccountBlockedException) {
            return new AtmOperationException(Reason.ACCOUNT_BLOCKED, error);
        }
        if (error instanceof InvalidPinException) {
            return new AtmOperationException(Reason.INVALID_PIN, error);
        }
        if (error instanceof InsufficientFundsException) {
            return new AtmOperationException(Reason.INSUFFICIENT_FUNDS, error);
        }
        if (error instanceof DailyLimitExceededException) {
            return new AtmOperationException(Reason.DAILY_LIMIT_EXCEEDED, error);
        }
        if (error instanceof IllegalArgumentException) {
            return new AtmOperationException(Reason.INVALID_OPERATION, error);
        }
        return error;
    }

    private static AccountViewDTO toAccountView(Account account) {
        Money remainingDailyLimit = account.getDailyWithdrawalLimit().minus(account.getTotalWithdrawnToday());
        return new AccountViewDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance().getAmount(),
                account.getBalance().format(),
                remainingDailyLimit.getAmount(),
                remainingDailyLimit.format(),
                account.isBlocked());
    }

    private static TransactionViewDTO toTransactionView(Transaction transaction) {
        return new TransactionViewDTO(
                transaction.getId(),
                transaction.getTimestamp(),
                transaction.getType().name(),
                transaction.getType().getDescription(),
                transaction.getAmount().getAmount(),
                transaction.getAmount().format(),
                transaction.getDescription());
    }
}
