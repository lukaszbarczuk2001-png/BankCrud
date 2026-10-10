package bank.repository;

import bank.model.Transaction;
import bank.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Historia operacji na kontach (tylko dopisywanie i odczyt). */
public class TransactionRepository {

    private final Connection conn;

    public TransactionRepository(Connection conn) {
        this.conn = conn;
    }

    public void add(long accountId, TransactionType type, long amountCents,
                    String counterparty, String title, long balanceAfterCents) throws SQLException {
        String sql = """
                INSERT INTO transactions (account_id, type, amount_cents, counterparty, title, balance_after_cents)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setString(2, type.name());
            ps.setLong(3, amountCents);
            ps.setString(4, counterparty);
            ps.setString(5, title);
            ps.setLong(6, balanceAfterCents);
            ps.executeUpdate();
        }
    }

    /** Ostatnie operacje na koncie, najnowsze pierwsze. */
    public List<Transaction> findByAccount(long accountId, int limit) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM transactions WHERE account_id = ? ORDER BY id DESC LIMIT ?")) {
            ps.setLong(1, accountId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Transaction> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Transaction(
                            rs.getLong("id"),
                            rs.getLong("account_id"),
                            TransactionType.valueOf(rs.getString("type")),
                            rs.getLong("amount_cents"),
                            rs.getString("counterparty"),
                            rs.getString("title"),
                            rs.getLong("balance_after_cents"),
                            rs.getString("created_at")));
                }
                return list;
            }
        }
    }
}
