package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.database.ConnectionFactory;
import com.fiap.bank.atm.infrastructure.database.DatabaseInitializer;
import com.fiap.bank.atm.infrastructure.exception.DatabaseException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AccountRepositoryJdbcImpl implements AccountRepository {

    private static final String AGENCIA_PADRAO = "0001";
    private static final String STATUS_ATIVA = "ACTIVE";
    private static final String STATUS_BLOQUEADA = "BLOCKED";

    private static final String SELECT_CONTA_POR_NUMERO = """
            SELECT id, agency, number, balance, status
            FROM tb_account
            WHERE number = ?""";

    private static final String SELECT_CONTA_POR_ID = """
            SELECT id, agency, number, balance, status
            FROM tb_account
            WHERE id = ?""";

    private static final String SELECT_TODAS_AS_CONTAS = """
            SELECT id, agency, number, balance, status
            FROM tb_account
            ORDER BY number""";

    private static final String SELECT_TRANSACOES_DA_CONTA = """
            SELECT id, account_id, type, amount, created_at
            FROM tb_transaction
            WHERE account_id = ?
            ORDER BY created_at DESC""";

    private static final String SELECT_IDS_DAS_TRANSACOES = """
            SELECT id
            FROM tb_transaction
            WHERE account_id = ?""";

    private static final String SELECT_CARTAO_DA_CONTA = """
            SELECT pin, daily_limit, failed_attempts
            FROM tb_card
            WHERE account_id = ?""";

    private static final String INSERT_CONTA = """
            INSERT INTO tb_account (id, agency, number, balance, status)
            VALUES (?, ?, ?, ?, ?)""";

    private static final String INSERT_CARTAO = """
            INSERT INTO tb_card (account_id, pin, daily_limit, failed_attempts)
            VALUES (?, ?, ?, ?)""";

    private static final String INSERT_TRANSACAO = """
            INSERT INTO tb_transaction (id, account_id, type, amount, created_at)
            VALUES (?, ?, ?, ?, ?)""";

    private static final String UPDATE_CONTA = """
            UPDATE tb_account
            SET balance = ?, status = ?
            WHERE id = ?""";

    private static final String UPDATE_CARTAO = """
            UPDATE tb_card
            SET failed_attempts = ?
            WHERE account_id = ?""";

    private static final String DELETE_TRANSACOES_DA_CONTA = """
            DELETE FROM tb_transaction
            WHERE account_id = ?""";

    private static final String DELETE_CARTAO = """
            DELETE FROM tb_card
            WHERE account_id = ?""";

    private static final String DELETE_CONTA = """
            DELETE FROM tb_account
            WHERE id = ?""";

    private final ConnectionFactory connectionFactory;

    public AccountRepositoryJdbcImpl() {
        this.connectionFactory = ConnectionFactory.getInstance();
        new DatabaseInitializer(connectionFactory).inicializar();
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return buscarConta(SELECT_CONTA_POR_NUMERO, formatarNumeroDaConta(accountNumber));
    }

    @Override
    public Optional<Account> buscarPorId(UUID id) {
        return buscarConta(SELECT_CONTA_POR_ID, id.toString());
    }

    @Override
    public List<Account> buscarTodos() {
        Connection conexao = connectionFactory.abrirConexao();
        try {
            return buscarLinhasDeContas(conexao).stream()
                    .map(linha -> montarConta(conexao, linha))
                    .toList();
        } finally {
            connectionFactory.fecharConexao(conexao);
        }
    }

    @Override
    public void salvar(Account conta) {
        Connection conexao = connectionFactory.abrirConexao();
        try {
            conexao.setAutoCommit(false);
            if (atualizarConta(conexao, conta)) {
                atualizarCartao(conexao, conta);
            } else {
                inserirConta(conexao, conta);
                inserirCartao(conexao, conta);
            }
            inserirTransacoesNovas(conexao, conta);
            conexao.commit();
        } catch (SQLException | DatabaseException e) {
            DatabaseException erro = new DatabaseException(
                    "Erro ao salvar a conta " + conta.getAccountNumber() + " no banco de dados.", e);
            desfazer(conexao, erro);
            throw erro;
        } finally {
            connectionFactory.fecharConexao(conexao);
        }
    }

    @Override
    public void remover(UUID id) {
        Connection conexao = connectionFactory.abrirConexao();
        try {
            conexao.setAutoCommit(false);
            // Filhos primeiro por causa das FOREIGN KEY
            List.of(DELETE_TRANSACOES_DA_CONTA, DELETE_CARTAO, DELETE_CONTA)
                    .forEach(sql -> excluir(conexao, sql, id));
            conexao.commit();
        } catch (SQLException | DatabaseException e) {
            DatabaseException erro = new DatabaseException("Erro ao remover a conta do banco de dados.", e);
            desfazer(conexao, erro);
            throw erro;
        } finally {
            connectionFactory.fecharConexao(conexao);
        }
    }

    private Optional<Account> buscarConta(String sql, String parametro) {
        Connection conexao = connectionFactory.abrirConexao();
        try {
            return buscarLinhaDeConta(conexao, sql, parametro)
                    .map(linha -> montarConta(conexao, linha));
        } finally {
            connectionFactory.fecharConexao(conexao);
        }
    }

    private Optional<LinhaConta> buscarLinhaDeConta(Connection conexao, String sql, String parametro) {
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, parametro);
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(lerLinhaDeConta(resultado));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar a conta no banco de dados.", e);
        }
    }

    private List<LinhaConta> buscarLinhasDeContas(Connection conexao) {
        try (PreparedStatement comando = conexao.prepareStatement(SELECT_TODAS_AS_CONTAS);
                ResultSet resultado = comando.executeQuery()) {
            List<LinhaConta> linhas = new ArrayList<>();
            while (resultado.next()) {
                linhas.add(lerLinhaDeConta(resultado));
            }
            return linhas;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar as contas no banco de dados.", e);
        }
    }

    private LinhaConta lerLinhaDeConta(ResultSet resultado) throws SQLException {
        return new LinhaConta(
                resultado.getString("id"),
                resultado.getString("number"),
                lerValor(resultado, "balance"),
                resultado.getString("status"));
    }

    private Optional<DadosCartao> buscarCartao(Connection conexao, String idConta) {
        try (PreparedStatement comando = conexao.prepareStatement(SELECT_CARTAO_DA_CONTA)) {
            comando.setString(1, idConta);
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(new DadosCartao(
                            resultado.getString("pin"),
                            lerValor(resultado, "daily_limit"),
                            resultado.getInt("failed_attempts")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar o cartão da conta no banco de dados.", e);
        }
    }

    private List<Transaction> buscarTransacoes(Connection conexao, String idConta) {
        try (PreparedStatement comando = conexao.prepareStatement(SELECT_TRANSACOES_DA_CONTA)) {
            comando.setString(1, idConta);
            try (ResultSet resultado = comando.executeQuery()) {
                List<Transaction> transacoes = new ArrayList<>();
                while (resultado.next()) {
                    TransactionType tipo = TransactionType.valueOf(resultado.getString("type"));
                    transacoes.add(new Transaction(
                            UUID.fromString(resultado.getString("id")),
                            resultado.getTimestamp("created_at").toLocalDateTime(),
                            tipo,
                            Money.of(lerValor(resultado, "amount")),
                            tipo.getDescription()));
                }
                return transacoes;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar as transações da conta no banco de dados.", e);
        }
    }

    private Account montarConta(Connection conexao, LinhaConta linha) {
        DadosCartao cartao = buscarCartao(conexao, linha.id())
                .orElseThrow(() -> new DatabaseException(
                        "Cartão da conta " + linha.numero() + " não encontrado no banco de dados."));
        List<Transaction> transacoes = buscarTransacoes(conexao, linha.id());

        // Total sacado hoje, usado no limite diário
        Money sacadoHoje = transacoes.stream()
                .filter(transacao -> transacao.getType() == TransactionType.WITHDRAWAL)
                .filter(transacao -> transacao.getTimestamp().toLocalDate().equals(LocalDate.now()))
                .map(Transaction::getAmount)
                .reduce(Money.ZERO, Money::plus);

        Account conta = Account.restore(
                UUID.fromString(linha.id()),
                linha.numero(),
                cartao.pin(),
                Money.of(linha.saldo()),
                Money.of(cartao.limiteDiario()),
                sacadoHoje,
                STATUS_BLOQUEADA.equals(linha.status()),
                cartao.tentativasErradas());

        // A consulta vem em ordem decrescente
        transacoes.reversed().forEach(conta::seedTransaction);
        return conta;
    }

    // O teclado do caixa não tem hífen: 123456 vira 12345-6
    private String formatarNumeroDaConta(String numero) {
        if (numero.contains("-") || numero.length() < 2) {
            return numero;
        }
        Integer posicaoDoDigito = numero.length() - 1;
        return numero.substring(0, posicaoDoDigito) + "-" + numero.substring(posicaoDoDigito);
    }

    private BigDecimal lerValor(ResultSet resultado, String coluna) throws SQLException {
        return resultado.getBigDecimal(coluna).setScale(2, RoundingMode.HALF_EVEN);
    }

    private Boolean atualizarConta(Connection conexao, Account conta) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(UPDATE_CONTA)) {
            comando.setBigDecimal(1, conta.getBalance().getAmount());
            comando.setString(2, statusDa(conta));
            comando.setString(3, conta.getId().toString());
            Integer linhasAlteradas = comando.executeUpdate();
            return linhasAlteradas > 0;
        }
    }

    private void inserirConta(Connection conexao, Account conta) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(INSERT_CONTA)) {
            comando.setString(1, conta.getId().toString());
            comando.setString(2, AGENCIA_PADRAO);
            comando.setString(3, conta.getAccountNumber());
            comando.setBigDecimal(4, conta.getBalance().getAmount());
            comando.setString(5, statusDa(conta));
            comando.executeUpdate();
        }
    }

    private void atualizarCartao(Connection conexao, Account conta) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(UPDATE_CARTAO)) {
            comando.setInt(1, conta.getFailedAttempts());
            comando.setString(2, conta.getId().toString());
            comando.executeUpdate();
        }
    }

    private void inserirCartao(Connection conexao, Account conta) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(INSERT_CARTAO)) {
            comando.setString(1, conta.getId().toString());
            comando.setString(2, conta.getPin());
            comando.setBigDecimal(3, conta.getDailyWithdrawalLimit().getAmount());
            comando.setInt(4, conta.getFailedAttempts());
            comando.executeUpdate();
        }
    }

    // Insere só as transações que ainda não estão no banco
    private void inserirTransacoesNovas(Connection conexao, Account conta) throws SQLException {
        Set<String> idsGravados = buscarIdsDasTransacoes(conexao, conta.getId().toString());
        List<Transaction> novas = conta.getTransactions().stream()
                .filter(transacao -> !idsGravados.contains(transacao.getId().toString()))
                .toList();
        if (novas.isEmpty()) {
            return;
        }
        try (PreparedStatement comando = conexao.prepareStatement(INSERT_TRANSACAO)) {
            novas.forEach(transacao -> adicionarAoLote(comando, conta, transacao));
            comando.executeBatch();
        }
    }

    private Set<String> buscarIdsDasTransacoes(Connection conexao, String idConta) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(SELECT_IDS_DAS_TRANSACOES)) {
            comando.setString(1, idConta);
            try (ResultSet resultado = comando.executeQuery()) {
                Set<String> ids = new HashSet<>();
                while (resultado.next()) {
                    ids.add(resultado.getString("id"));
                }
                return ids;
            }
        }
    }

    private void adicionarAoLote(PreparedStatement comando, Account conta, Transaction transacao) {
        try {
            comando.setString(1, transacao.getId().toString());
            comando.setString(2, conta.getId().toString());
            comando.setString(3, transacao.getType().name());
            comando.setBigDecimal(4, transacao.getAmount().getAmount());
            comando.setTimestamp(5, Timestamp.valueOf(transacao.getTimestamp()));
            comando.addBatch();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao preparar a gravação da transação.", e);
        }
    }

    private void excluir(Connection conexao, String sql, UUID id) {
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, id.toString());
            comando.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao excluir os dados da conta.", e);
        }
    }

    private void desfazer(Connection conexao, DatabaseException erro) {
        try {
            conexao.rollback();
        } catch (SQLException falhaNoRollback) {
            erro.addSuppressed(falhaNoRollback);
        }
    }

    private String statusDa(Account conta) {
        return conta.isBlocked() ? STATUS_BLOQUEADA : STATUS_ATIVA;
    }

    private record LinhaConta(String id, String numero, BigDecimal saldo, String status) {
    }

    private record DadosCartao(String pin, BigDecimal limiteDiario, Integer tentativasErradas) {
    }
}
