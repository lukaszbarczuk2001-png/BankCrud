package bank.service;

import bank.BankException;
import bank.db.Database;
import bank.model.Account;
import bank.model.User;
import bank.repository.AccountRepository;
import bank.repository.UserRepository;
import bank.util.PasswordHasher;

import java.sql.SQLException;
import java.util.regex.Pattern;

/** Rejestracja, logowanie i zarządzanie profilem użytkownika. */
public class UserService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_.]{3,20}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int MIN_PASSWORD = 8;

    private final Database db;
    private final UserRepository users;
    private final AccountRepository accounts;

    public UserService(Database db, UserRepository users, AccountRepository accounts) {
        this.db = db;
        this.users = users;
        this.accounts = accounts;
    }

    public User register(String username, String fullName, String email, String password) throws SQLException {
        username = username.trim();
        fullName = fullName.trim();
        email = email.trim();

        if (!USERNAME.matcher(username).matches()) {
            throw new BankException("Login: 3–20 znaków, tylko litery, cyfry, _ i kropka.");
        }
        validateProfile(fullName, email);
        validatePassword(password);
        if (users.usernameTaken(username)) {
            throw new BankException("Login \"" + username + "\" jest już zajęty.");
        }
        if (users.emailTaken(email, -1)) {
            throw new BankException("Ten e-mail jest już zarejestrowany.");
        }

        long id = users.create(username, fullName, email, PasswordHasher.hash(password));
        return users.findById(id).orElseThrow();
    }

    public User login(String username, String password) throws SQLException {
        // Ten sam komunikat w obu przypadkach, żeby nie zdradzać, czy login istnieje.
        User user = users.findByUsername(username.trim())
                .orElseThrow(() -> new BankException("Nieprawidłowy login lub hasło."));
        if (!PasswordHasher.verify(password, user.passwordHash())) {
            throw new BankException("Nieprawidłowy login lub hasło.");
        }
        return user;
    }

    public User updateProfile(User user, String fullName, String email) throws SQLException {
        fullName = fullName.trim();
        email = email.trim();
        validateProfile(fullName, email);
        if (users.emailTaken(email, user.id())) {
            throw new BankException("Ten e-mail jest już używany przez inne konto.");
        }
        users.updateProfile(user.id(), fullName, email);
        return users.findById(user.id()).orElseThrow();
    }

    public void changePassword(User user, String oldPassword, String newPassword) throws SQLException {
        User current = users.findById(user.id()).orElseThrow();
        if (!PasswordHasher.verify(oldPassword, current.passwordHash())) {
            throw new BankException("Obecne hasło jest nieprawidłowe.");
        }
        validatePassword(newPassword);
        users.updatePassword(user.id(), PasswordHasher.hash(newPassword));
    }

    /** Usuwa użytkownika razem z kontami – tylko jeśli wszystkie konta mają saldo 0. */
    public void deleteUser(User user, String password) throws SQLException {
        User current = users.findById(user.id()).orElseThrow();
        if (!PasswordHasher.verify(password, current.passwordHash())) {
            throw new BankException("Nieprawidłowe hasło.");
        }
        db.inTransaction(() -> {
            for (Account a : accounts.findByUser(user.id())) {
                if (a.balanceCents() != 0) {
                    throw new BankException("Najpierw wypłać lub przelej wszystkie środki ze swoich kont.");
                }
            }
            users.delete(user.id());
            return null;
        });
    }

    private static void validateProfile(String fullName, String email) {
        if (fullName.length() < 3) {
            throw new BankException("Podaj imię i nazwisko.");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw new BankException("Nieprawidłowy adres e-mail.");
        }
    }

    private static void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD) {
            throw new BankException("Hasło musi mieć co najmniej " + MIN_PASSWORD + " znaków.");
        }
        if (!password.matches(".*\\d.*") || !password.matches(".*[A-Za-z].*")) {
            throw new BankException("Hasło musi zawierać co najmniej jedną literę i jedną cyfrę.");
        }
    }
}
