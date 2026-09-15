package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de persistência baseada em arquivo texto local.
 * Substitui a antiga persistência em memória (InMemoryAccountRepository),
 * garantindo que os dados sobrevivam a reinicializações da aplicação.
 */
public class FileAccountRepository implements AccountRepository {

    private static final Path DATA_FILE = Path.of("atm-data.txt");
    private static final String SEP = "|";

    private final Map<String, Account> accounts = new HashMap<>();

    public FileAccountRepository() {
        if (Files.exists(DATA_FILE)) {
            load();
        } else {
            seedData();
            persistAll();
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    @Override
    public void save(Account account) {
        accounts.put(account.getAccountNumber(), account);
        persistAll();
    }

    private void persistAll() {
        List<String> lines = new java.util.ArrayList<>();
        for (Account acc : accounts.values()) {
            lines.add(String.join(SEP,
                    "ACCOUNT",
                    acc.getId().toString(),
                    acc.getAccountNumber(),
                    acc.getPin(),
                    acc.getBalance().getAmount().toPlainString(),
                    acc.getDailyWithdrawalLimit().getAmount().toPlainString(),
                    acc.getTotalWithdrawnToday().getAmount().toPlainString(),
                    String.valueOf(acc.isBlocked()),
                    String.valueOf(acc.getFailedAttempts())));

            for (Transaction tx : acc.getTransactions()) {
                lines.add(String.join(SEP,
                        "TRANSACTION",
                        acc.getAccountNumber(),
                        tx.getId().toString(),
                        tx.getTimestamp().toString(),
                        tx.getType().name(),
                        tx.getAmount().getAmount().toPlainString(),
                        tx.getDescription()));
            }
        }
        try {
            Files.write(DATA_FILE, lines);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar dados do ATM em disco.", e);
        }
    }

    private void load() {
        try {
            for (String line : Files.readAllLines(DATA_FILE)) {
                String[] parts = line.split(java.util.regex.Pattern.quote(SEP), -1);
                if (parts[0].equals("ACCOUNT")) {
                    Account account = Account.restore(
                            UUID.fromString(parts[1]),
                            parts[2],
                            parts[3],
                            Money.of(new BigDecimal(parts[4])),
                            Money.of(new BigDecimal(parts[5])),
                            Money.of(new BigDecimal(parts[6])),
                            Boolean.parseBoolean(parts[7]),
                            Integer.parseInt(parts[8]));
                    accounts.put(account.getAccountNumber(), account);
                } else if (parts[0].equals("TRANSACTION")) {
                    Account account = accounts.get(parts[1]);
                    if (account != null) {
                        account.seedTransaction(new Transaction(
                                UUID.fromString(parts[2]),
                                LocalDateTime.parse(parts[3]),
                                TransactionType.valueOf(parts[4]),
                                Money.of(new BigDecimal(parts[5])),
                                parts[6]));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao carregar dados do ATM do disco.", e);
        }
    }

    private void seedData() {
        Account acc1 = new Account(UUID.randomUUID(), "12345", "1234", Money.of(5000.00), Money.of(1500.00));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(3),
                TransactionType.DEPOSIT, Money.of(2000.00), "Depósito em dinheiro"));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_IN, Money.of(500.00), "Transf. de Conta 67890"));
        acc1.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(1),
                TransactionType.WITHDRAWAL, Money.of(100.00), "Saque eletrônico"));
        accounts.put(acc1.getAccountNumber(), acc1);

        Account acc2 = new Account(UUID.randomUUID(), "67890", "5678", Money.of(1200.00), Money.of(1000.00));
        acc2.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(5),
                TransactionType.DEPOSIT, Money.of(1500.00), "Depósito inicial"));
        acc2.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(2),
                TransactionType.TRANSFER_OUT, Money.of(500.00), "Transf. para Conta 12345"));
        accounts.put(acc2.getAccountNumber(), acc2);

        Account acc3 = new Account(UUID.randomUUID(), "99999", "9999", Money.of(50.00), Money.of(500.00));
        acc3.seedTransaction(new Transaction(UUID.randomUUID(), LocalDateTime.now().minusDays(10),
                TransactionType.DEPOSIT, Money.of(50.00), "Abertura de conta"));
        accounts.put(acc3.getAccountNumber(), acc3);
    }
}