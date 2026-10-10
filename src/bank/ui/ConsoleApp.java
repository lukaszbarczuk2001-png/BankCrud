package bank.ui;

import bank.BankException;
import bank.db.Database;
import bank.model.Account;
import bank.model.Transaction;
import bank.model.User;
import bank.repository.AccountRepository;
import bank.repository.TransactionRepository;
import bank.repository.UserRepository;
import bank.service.AccountService;
import bank.service.UserService;
import bank.util.AccountNumbers;
import bank.util.Money;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/** Menu tekstowe aplikacji. */
public class ConsoleApp {

    private final String jdbcUrl;
    private final Scanner in = new Scanner(System.in);

    private UserService userService;
    private AccountService accountService;
    private User currentUser;

    public ConsoleApp(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void run() {
        try (Database db = new Database(jdbcUrl)) {
            UserRepository users = new UserRepository(db.connection());
            AccountRepository accounts = new AccountRepository(db.connection());
            TransactionRepository transactions = new TransactionRepository(db.connection());
            userService = new UserService(db, users, accounts);
            accountService = new AccountService(db, accounts, transactions);

            println("=== BankCrud ===");
            boolean running = true;
            while (running) {
                running = currentUser == null ? guestMenu() : userMenu();
            }
            println("Do widzenia!");
        } catch (SQLException e) {
            System.err.println("Nie udało się połączyć z bazą danych: " + e.getMessage());
        } catch (NoSuchElementException e) {
            // koniec wejścia (np. Ctrl+D) – po prostu kończymy
        }
    }

    // ---------------------------------------------------------------- menu

    private boolean guestMenu() {
        println("""

                1. Rejestracja
                2. Logowanie
                0. Wyjście""");
        switch (ask("Wybierz")) {
            case "1" -> action(this::register);
            case "2" -> action(this::login);
            case "0" -> { return false; }
            default -> println("Nieznana opcja.");
        }
        return true;
    }

    private boolean userMenu() {
        println("""

                --- Zalogowano jako %s ---
                1. Moje konta
                2. Otwórz nowe konto
                3. Wpłata
                4. Wypłata
                5. Przelew
                6. Historia konta
                7. Zamknij konto
                8. Mój profil / edycja danych
                9. Zmień hasło
                10. Usuń mój profil
                0. Wyloguj""".formatted(currentUser.username()));
        switch (ask("Wybierz")) {
            case "1" -> action(this::showAccounts);
            case "2" -> action(this::openAccount);
            case "3" -> action(this::deposit);
            case "4" -> action(this::withdraw);
            case "5" -> action(this::transfer);
            case "6" -> action(this::history);
            case "7" -> action(this::closeAccount);
            case "8" -> action(this::editProfile);
            case "9" -> action(this::changePassword);
            case "10" -> action(this::deleteProfile);
            case "0" -> {
                currentUser = null;
                println("Wylogowano.");
            }
            default -> println("Nieznana opcja.");
        }
        return true;
    }

    // ---------------------------------------------------------------- użytkownik

    private void register() throws SQLException {
        println("\n--- Rejestracja ---");
        String username = ask("Login");
        String fullName = ask("Imię i nazwisko");
        String email = ask("E-mail");
        String password = askPassword("Hasło (min. 8 znaków, litera i cyfra)");
        String repeat = askPassword("Powtórz hasło");
        if (!password.equals(repeat)) {
            throw new BankException("Hasła nie są takie same.");
        }
        User user = userService.register(username, fullName, email, password);
        currentUser = user;
        println("Konto użytkownika utworzone. Witaj, " + user.fullName() + "!");

        Account acc = accountService.openAccount(user.id());
        println("Otworzyliśmy Ci pierwszy rachunek: " + AccountNumbers.pretty(acc.number()));
    }

    private void login() throws SQLException {
        println("\n--- Logowanie ---");
        String username = ask("Login");
        String password = askPassword("Hasło");
        currentUser = userService.login(username, password);
        println("Witaj, " + currentUser.fullName() + "!");
    }

    private void editProfile() throws SQLException {
        println("\n--- Mój profil ---");
        println("Login:            " + currentUser.username());
        println("Imię i nazwisko:  " + currentUser.fullName());
        println("E-mail:           " + currentUser.email());
        println("Data rejestracji: " + currentUser.createdAt());
        if (!ask("Edytować dane? (t/n)").equalsIgnoreCase("t")) {
            return;
        }
        String fullName = askOrKeep("Nowe imię i nazwisko", currentUser.fullName());
        String email = askOrKeep("Nowy e-mail", currentUser.email());
        currentUser = userService.updateProfile(currentUser, fullName, email);
        println("Dane zapisane.");
    }

    private void changePassword() throws SQLException {
        String oldPassword = askPassword("Obecne hasło");
        String newPassword = askPassword("Nowe hasło");
        if (!newPassword.equals(askPassword("Powtórz nowe hasło"))) {
            throw new BankException("Hasła nie są takie same.");
        }
        userService.changePassword(currentUser, oldPassword, newPassword);
        println("Hasło zmienione.");
    }

    private void deleteProfile() throws SQLException {
        println("UWAGA: usunięcie profilu usuwa też wszystkie Twoje konta i ich historię.");
        if (!ask("Na pewno? Wpisz TAK, aby potwierdzić").equals("TAK")) {
            println("Anulowano.");
            return;
        }
        userService.deleteUser(currentUser, askPassword("Hasło"));
        currentUser = null;
        println("Profil został usunięty.");
    }

    // ---------------------------------------------------------------- konta

    private void showAccounts() throws SQLException {
        List<Account> list = accountService.listAccounts(currentUser.id());
        if (list.isEmpty()) {
            println("Nie masz żadnych kont. Wybierz opcję 2, aby otworzyć konto.");
            return;
        }
        long total = 0;
        println("\n--- Moje konta ---");
        for (int i = 0; i < list.size(); i++) {
            Account a = list.get(i);
            total += a.balanceCents();
            println("%d. %s   saldo: %s".formatted(i + 1, AccountNumbers.pretty(a.number()), Money.format(a.balanceCents())));
        }
        println("Razem: " + Money.format(total));
    }

    private void openAccount() throws SQLException {
        Account acc = accountService.openAccount(currentUser.id());
        println("Otwarto nowe konto: " + AccountNumbers.pretty(acc.number()));
    }

    private void deposit() throws SQLException {
        Optional<Account> acc = chooseAccount("Na które konto wpłacić?");
        if (acc.isEmpty()) return;
        long amount = Money.parse(ask("Kwota"));
        Account updated = accountService.deposit(currentUser.id(), acc.get().id(), amount);
        println("Wpłacono " + Money.format(amount) + ". Nowe saldo: " + Money.format(updated.balanceCents()));
    }

    private void withdraw() throws SQLException {
        Optional<Account> acc = chooseAccount("Z którego konta wypłacić?");
        if (acc.isEmpty()) return;
        long amount = Money.parse(ask("Kwota"));
        Account updated = accountService.withdraw(currentUser.id(), acc.get().id(), amount);
        println("Wypłacono " + Money.format(amount) + ". Nowe saldo: " + Money.format(updated.balanceCents()));
    }

    private void transfer() throws SQLException {
        Optional<Account> acc = chooseAccount("Z którego konta wysłać przelew?");
        if (acc.isEmpty()) return;
        String to = ask("Numer konta odbiorcy (26 cyfr)");
        long amount = Money.parse(ask("Kwota"));
        String title = ask("Tytuł przelewu");
        Account updated = accountService.transfer(currentUser.id(), acc.get().id(), to, amount, title);
        println("Przelew wysłany. Saldo po operacji: " + Money.format(updated.balanceCents()));
    }

    private void history() throws SQLException {
        Optional<Account> acc = chooseAccount("Historia którego konta?");
        if (acc.isEmpty()) return;
        List<Transaction> list = accountService.history(currentUser.id(), acc.get().id(), 20);
        if (list.isEmpty()) {
            println("Brak operacji na tym koncie.");
            return;
        }
        println("\n--- Ostatnie operacje (max 20) ---");
        for (Transaction t : list) {
            String sign = t.type().isDebit() ? "-" : "+";
            String party = t.counterparty() == null ? "" : "  [" + AccountNumbers.pretty(t.counterparty()) + "]";
            println("%s  %-20s %s%s  saldo: %s  \"%s\"%s".formatted(
                    t.createdAt(), t.type().label(), sign, Money.format(t.amountCents()),
                    Money.format(t.balanceAfterCents()), t.title(), party));
        }
    }

    private void closeAccount() throws SQLException {
        Optional<Account> acc = chooseAccount("Które konto zamknąć?");
        if (acc.isEmpty()) return;
        if (!ask("Zamknąć konto " + AccountNumbers.pretty(acc.get().number()) + "? (t/n)").equalsIgnoreCase("t")) {
            println("Anulowano.");
            return;
        }
        accountService.closeAccount(currentUser.id(), acc.get().id());
        println("Konto zamknięte.");
    }

    /** Wyświetla konta użytkownika i pozwala wybrać jedno numerem z listy. */
    private Optional<Account> chooseAccount(String prompt) throws SQLException {
        List<Account> list = accountService.listAccounts(currentUser.id());
        if (list.isEmpty()) {
            println("Nie masz żadnych kont. Najpierw otwórz konto (opcja 2).");
            return Optional.empty();
        }
        if (list.size() == 1) {
            return Optional.of(list.get(0));
        }
        println(prompt);
        for (int i = 0; i < list.size(); i++) {
            Account a = list.get(i);
            println("  %d. %s  (%s)".formatted(i + 1, AccountNumbers.pretty(a.number()), Money.format(a.balanceCents())));
        }
        String choice = ask("Numer z listy");
        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < list.size()) {
                return Optional.of(list.get(idx));
            }
        } catch (NumberFormatException ignored) {
            // obsłużone niżej
        }
        println("Nieprawidłowy wybór.");
        return Optional.empty();
    }

    // ---------------------------------------------------------------- pomocnicze

    @FunctionalInterface
    private interface Action {
        void run() throws SQLException;
    }

    /** Uruchamia akcję i zamienia błędy na czytelne komunikaty, żeby aplikacja się nie wysypała. */
    private void action(Action action) {
        try {
            action.run();
        } catch (BankException e) {
            println("! " + e.getMessage());
        } catch (SQLException e) {
            println("! Błąd bazy danych: " + e.getMessage());
        }
    }

    private String ask(String prompt) {
        System.out.print(prompt + ": ");
        return in.nextLine().trim();
    }

    private String askOrKeep(String prompt, String current) {
        String value = ask(prompt + " [Enter = " + current + "]");
        return value.isEmpty() ? current : value;
    }

    /**
     * Hasło czytamy zwykłym Scannerem (w konsoli IntelliJ ukrywanie znaków nie działa),
     * więc będzie widoczne na ekranie. Nie przycinamy spacji – są częścią hasła.
     */
    private String askPassword(String prompt) {
        System.out.print(prompt + ": ");
        return in.nextLine();
    }

    private static void println(String text) {
        System.out.println(text);
    }
}
