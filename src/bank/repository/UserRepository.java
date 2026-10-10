package bank.repository;

import bank.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** CRUD na tabeli users. */
public class UserRepository {

    private final Connection conn;

    public UserRepository(Connection conn) {
        this.conn = conn;
    }

    // CREATE
    public long create(String username, String fullName, String email, String passwordHash) throws SQLException {
        // RETURNING id zwraca nadane ID (sterownik SQLite nie wspiera getGeneratedKeys)
        String sql = "INSERT INTO users (username, full_name, email, password_hash) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, passwordHash);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // READ
    public Optional<User> findById(long id) throws SQLException {
        return findOne("SELECT * FROM users WHERE id = ?", id);
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        return findOne("SELECT * FROM users WHERE username = ? COLLATE NOCASE", username);
    }

    public boolean usernameTaken(String username) throws SQLException {
        return findByUsername(username).isPresent();
    }

    /** Czy e-mail jest zajęty przez kogoś innego niż exceptUserId. */
    public boolean emailTaken(String email, long exceptUserId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ? COLLATE NOCASE AND id <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setLong(2, exceptUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // UPDATE
    public void updateProfile(long id, String fullName, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET full_name = ?, email = ? WHERE id = ?")) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void updatePassword(long id, String passwordHash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    // DELETE (konta i historia usuwają się kaskadowo)
    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Optional<User> findOne(String sql, Object param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("created_at"));
    }
}
