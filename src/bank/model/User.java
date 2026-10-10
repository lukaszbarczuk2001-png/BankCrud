package bank.model;

public record User(long id, String username, String fullName, String email,
                   String passwordHash, String createdAt) {
}
