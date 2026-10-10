package bank.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Jedno połączenie z bazą SQLite + tworzenie tabel + obsługa transakcji. */
public final class Database implements AutoCloseable {

    private final Connection connection;

    public Database(String url) throws SQLException {
        this.connection = DriverManager.getConnection(url);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON"); // SQLite domyślnie nie pilnuje kluczy obcych
        }
        createSchema();
    }

    public Connection connection() {
        return connection;
    }

    private void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE,
                    full_name     TEXT    NOT NULL,
                    email         TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    created_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    number        TEXT    NOT NULL UNIQUE,
                    user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    balance_cents INTEGER NOT NULL DEFAULT 0 CHECK (balance_cents >= 0),
                    created_at    TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_id          INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
                    type                TEXT    NOT NULL,
                    amount_cents        INTEGER NOT NULL,
                    counterparty        TEXT,
                    title               TEXT,
                    balance_after_cents INTEGER NOT NULL,
                    created_at          TEXT    NOT NULL DEFAULT (datetime('now', 'localtime'))
                )""");
        }
    }

    /** Wykonuje kilka operacji jako jedną transakcję: albo wszystkie, albo żadna. */
    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        connection.setAutoCommit(false);
        try {
            T result = work.run();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run() throws SQLException;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
