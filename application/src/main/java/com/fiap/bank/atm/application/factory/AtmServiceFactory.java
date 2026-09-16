package com.fiap.bank.atm.application.factory;

import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import java.util.ServiceLoader;

public final class AtmServiceFactory {

    private AtmServiceFactory() {
    }

    public static AtmService createAtmService() {
        // Implementação registrada em META-INF/services no módulo infrastructure
        AccountRepository accountRepository = ServiceLoader
                .load(AccountRepository.class, AccountRepository.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhuma implementação de AccountRepository foi encontrada no módulo infrastructure."));
        return new AtmService(accountRepository);
    }
}
