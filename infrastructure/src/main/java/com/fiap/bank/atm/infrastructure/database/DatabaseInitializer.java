package com.fiap.bank.atm.infrastructure.database;

import com.fiap.bank.atm.infrastructure.exception.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DatabaseInitializer {

    // Tabelas do sistema
    private static final String CREATE_TB_ACCOUNT = """
            CREATE TABLE IF NOT EXISTS tb_account (
                id VARCHAR(36) PRIMARY KEY,
                agency VARCHAR(10) NOT NULL,
                number VARCHAR(20) NOT NULL,
                balance DECIMAL(15, 2) NOT NULL,
                status VARCHAR(20) NOT NULL
            )""";

    private static final String CREATE_TB_TRANSACTION = """
            CREATE TABLE IF NOT EXISTS tb_transaction (
                id VARCHAR(36) PRIMARY KEY,
                account_id VARCHAR(36) NOT NULL,
                type VARCHAR(20) NOT NULL,
                amount DECIMAL(15, 2) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                FOREIGN KEY (account_id) REFERENCES tb_account(id)
            )""";

    // Senha, limite diário e tentativas de senha de cada conta
    private static final String CREATE_TB_CARD = """
            CREATE TABLE IF NOT EXISTS tb_card (
                account_id VARCHAR(36) PRIMARY KEY,
                pin VARCHAR(4) NOT NULL,
                daily_limit DECIMAL(15, 2) NOT NULL,
                failed_attempts INTEGER NOT NULL,
                FOREIGN KEY (account_id) REFERENCES tb_account(id)
            )""";

    // Carga inicial das contas de teste e dos seus cartões
    private static final List<String> CARGA_INICIAL = List.of(
            """
            INSERT INTO tb_account (id, agency, number, balance, status)
            VALUES ('550e8400-e29b-41d4-a716-446655440000', '0001', '12345-6', 1500.00, 'ACTIVE')
            ON CONFLICT DO NOTHING""",
            """
            INSERT INTO tb_account (id, agency, number, balance, status)
            VALUES ('550e8400-e29b-41d4-a716-446655440001', '0001', '98765-4', 250.50, 'ACTIVE')
            ON CONFLICT DO NOTHING""",
            """
            INSERT INTO tb_account (id, agency, number, balance, status)
            VALUES ('550e8400-e29b-41d4-a716-446655440002', '0002', '11111-1', 0.00, 'BLOCKED')
            ON CONFLICT DO NOTHING""",
            """
            INSERT INTO tb_card (account_id, pin, daily_limit, failed_attempts)
            VALUES ('550e8400-e29b-41d4-a716-446655440000', '1234', 1500.00, 0)
            ON CONFLICT DO NOTHING""",
            """
            INSERT INTO tb_card (account_id, pin, daily_limit, failed_attempts)
            VALUES ('550e8400-e29b-41d4-a716-446655440001', '5678', 1000.00, 0)
            ON CONFLICT DO NOTHING""",
            """
            INSERT INTO tb_card (account_id, pin, daily_limit, failed_attempts)
            VALUES ('550e8400-e29b-41d4-a716-446655440002', '9999', 500.00, 3)
            ON CONFLICT DO NOTHING""");

    private final ConnectionFactory connectionFactory;

    public DatabaseInitializer(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void inicializar() {
        Connection conexao = connectionFactory.abrirConexao();
        try {
            conexao.setAutoCommit(false);
            List.of(CREATE_TB_ACCOUNT, CREATE_TB_TRANSACTION, CREATE_TB_CARD)
                    .forEach(sql -> executar(conexao, sql));
            CARGA_INICIAL.forEach(sql -> executar(conexao, sql));
            conexao.commit();
        } catch (SQLException | DatabaseException e) {
            DatabaseException erro = new DatabaseException("Não foi possível preparar o banco de dados.", e);
            desfazer(conexao, erro);
            throw erro;
        } finally {
            connectionFactory.fecharConexao(conexao);
        }
    }

    private void executar(Connection conexao, String sql) {
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao executar o SQL de inicialização do banco de dados.", e);
        }
    }

    private void desfazer(Connection conexao, DatabaseException erro) {
        try {
            conexao.rollback();
        } catch (SQLException falhaNoRollback) {
            erro.addSuppressed(falhaNoRollback);
        }
    }
}
