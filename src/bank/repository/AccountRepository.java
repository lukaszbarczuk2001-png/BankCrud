package bank.repository;

import bank.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD na tabeli accounts. */
public class AccountRepository {

    private final Connection conn;

    public AccountRepository(Connection conn) {
        this.conn = conn;
    }

    // CREATE
    public long create(long userId, String number) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO accounts (user_id, number) VALUES (?, ?) RETURNING id")) {
            ps.setLong(1, userId);
            ps.setString(2, number);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // READ
    public Optional<Account> findById(long id) throws SQLException {
        return findOne("SELECT * FROM accounts WHERE id = ?", id);
    }

    public Optional<Account> findByNumber(String number) throws SQLException {
        return findOne("SELECT * FROM accounts WHERE number = ?", number);
    }

    public List<Account> findByUser(long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM accounts WHERE user_id = ? ORDER BY id")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Account> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    // UPDATE
    /**
     * Zmienia saldo o delta groszy. Zwraca false, jeśli saldo spadłoby poniżej zera
     * (warunek sprawdzany w samym UPDATE, więc nie da się "przepłacić").
     */
    public boolean changeBalance(long accountId, long deltaCents) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE accounts SET balance_cents = balance_cents + ? WHERE id = ? AND balance_cents + ? >= 0")) {
            ps.setLong(1, deltaCents);
            ps.setLong(2, accountId);
            ps.setLong(3, deltaCents);
            return ps.executeUpdate() == 1;
        }
    }

    // DELETE
    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM accounts WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Optional<Account> findOne(String sql, Object param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static Account map(ResultSet rs) throws SQLException {
        return new Account(
                rs.getLong("id"),
                rs.getString("number"),
                rs.getLong("user_id"),
                rs.getLong("balance_cents"),
                rs.getString("created_at"));
    }
}
