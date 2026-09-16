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

        // MELHORIA 1: try/finally no lugar de accountRepository.salvar(account)
        // duplicado no try e no catch. O estado da conta precisa ser gravado
        // tanto no sucesso (zeramento de tentativas) quanto na falha (tentativa
        // errada / bloqueio), então o finally cobre os dois casos numa linha só.
        try {
            account.authenticate(pin);
            currentAccount = account;
            return toAccountView(account);
        } catch (RuntimeException e) {
            throw translate(e);
        } finally {
            accountRepository.salvar(account);
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

    // MELHORIA 2: switch com pattern matching (Java 21) no lugar da cadeia de
    // if/instanceof. Mesmo comportamento, mas mais legível e mais alinhado
    // com a versão do Java que o projeto já usa.
    private static RuntimeException translate(RuntimeException error) {
        return switch (error) {
            case AccountBlockedException e -> new AtmOperationException(Reason.ACCOUNT_BLOCKED, e);
            case InvalidPinException e -> new AtmOperationException(Reason.INVALID_PIN, e);
            case InsufficientFundsException e -> new AtmOperationException(Reason.INSUFFICIENT_FUNDS, e);
            case DailyLimitExceededException e -> new AtmOperationException(Reason.DAILY_LIMIT_EXCEEDED, e);
            case IllegalArgumentException e -> new AtmOperationException(Reason.INVALID_OPERATION, e);
            default -> error;
        };
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