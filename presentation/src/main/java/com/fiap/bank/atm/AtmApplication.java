package com.fiap.bank.atm;

import com.fiap.bank.atm.application.factory.AtmServiceFactory;
import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.presentation.AtmFrame;
import javax.swing.SwingUtilities;

public class AtmApplication {
    public static void main(String[] args) {
        // Inicializa as camadas de Infraestrutura e Aplicação (DDD)
        AtmService atmService = AtmServiceFactory.createAtmService();

        // Inicializa a camada de Apresentação de forma segura na Event Dispatch Thread
        // (EDT)
        SwingUtilities.invokeLater(() -> {
            AtmFrame mainFrame = new AtmFrame(atmService);
            mainFrame.setVisible(true);
        });
    }
}
