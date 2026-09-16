package com.fiap.bank.atm.infrastructure.database;

import com.fiap.bank.atm.infrastructure.exception.DatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConnectionFactory {

    private static final String DRIVER = "org.sqlite.JDBC";
    private static final String URL = "jdbc:sqlite:fiap-bank-atm.db";

    private static ConnectionFactory instance;

    private final Properties configuracao;

    private ConnectionFactory() {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("Driver JDBC do SQLite não encontrado.", e);
        }
        configuracao = new Properties();
        // O SQLite só aplica as FOREIGN KEY com essa opção ligada
        configuracao.setProperty("foreign_keys", "true");
        // Grava as datas como texto (yyyy-MM-dd HH:mm:ss.SSS)
        configuracao.setProperty("date_class", "TEXT");
    }

    public static synchronized ConnectionFactory getInstance() {
        if (instance == null) {
            instance = new ConnectionFactory();
        }
        return instance;
    }

    public Connection abrirConexao() {
        try {
            return DriverManager.getConnection(URL, configuracao);
        } catch (SQLException e) {
            throw new DatabaseException("Não foi possível abrir a conexão com o banco de dados.", e);
        }
    }

    public void fecharConexao(Connection conexao) {
        try {
            conexao.close();
        } catch (SQLException e) {
            throw new DatabaseException("Não foi possível fechar a conexão com o banco de dados.", e);
        }
    }
}
