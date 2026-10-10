package bank.service;

import bank.BankException;
import bank.db.Database;
import bank.model.Account;
import bank.model.Transaction;
import bank.model.TransactionType;
import bank.repository.AccountRepository;
import bank.repository.TransactionRepository;
import bank.util.AccountNumbers;

import java.sql.SQLException;
import java.util.List;

/** Konta bankowe i operacje na pieniądzach. */
public class AccountService {

    private static final int MAX_ACCOUNTS_PER_USER = 5;

    private final Database db;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public AccountService(Database db, AccountRepository accounts, TransactionRepository transactions) {
        this.db = db;
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public Account openAccount(long userId) throws SQLException {
        if (accounts.findByUser(userId).size() >= MAX_ACCOUNTS_PER_USER) {
            throw new BankException("Możesz mieć maksymalnie " + MAX_ACCOUNTS_PER_USER + " kont.");
        }
        String number;
        do {
            number = AccountNumbers.generate();
        } while (accounts.findByNumber(number).isPresent());
        long id = accounts.create(userId, number);
        return accounts.findById(id).orElseThrow();
    }

    public List<Account> listAccounts(long userId) throws SQLException {
        return accounts.findByUser(userId);
    }

    public Account deposit(long userId, long accountId, long amountCents) throws SQLException {
        return db.inTransaction(() -> {
            Account acc = owned(userId, accountId);
            accounts.changeBalance(acc.id(), amountCents);
            Account updated = accounts.findById(acc.id()).orElseThrow();
            transactions.add(acc.id(), TransactionType.DEPOSIT, amountCents, null, "Wpłata gotówkowa",
                    updated.balanceCents());
            return updated;
        });
    }

    public Account withdraw(long userId, long accountId, long amountCents) throws SQLException {
        return db.inTransaction(() -> {
            Account acc = owned(userId, accountId);
            if (!accounts.changeBalance(acc.id(), -amountCents)) {
                throw new BankException("Brak wystarczających środków na koncie.");
            }
            Account updated = accounts.findById(acc.id()).orElseThrow();
            transactions.add(acc.id(), TransactionType.WITHDRAWAL, amountCents, null, "Wypłata gotówkowa",
                    updated.balanceCents());
            return updated;
        });
    }

    /** Przelew na dowolne konto w naszym banku (także na własne). Wszystko w jednej transakcji. */
    public Account transfer(long userId, long fromAccountId, String toNumberRaw, long amountCents, String title)
            throws SQLException {
        String toNumber = AccountNumbers.normalize(toNumberRaw);
        if (!AccountNumbers.isValid(toNumber)) {
            throw new BankException("Nieprawidłowy numer konta (błędna długość lub suma kontrolna).");
        }
        String finalTitle = title == null || title.isBlank() ? "Przelew" : title.trim();

        return db.inTransaction(() -> {
            Account from = owned(userId, fromAccountId);
            Account to = accounts.findByNumber(toNumber)
                    .orElseThrow(() -> new BankException("Nie ma takiego konta w naszym banku."));
            if (from.id() == to.id()) {
                throw new BankException("Nie można zrobić przelewu na to samo konto.");
            }
            if (!accounts.changeBalance(from.id(), -amountCents)) {
                throw new BankException("Brak wystarczających środków na koncie.");
            }
            accounts.changeBalance(to.id(), amountCents);

            Account fromAfter = accounts.findById(from.id()).orElseThrow();
            Account toAfter = accounts.findById(to.id()).orElseThrow();
            transactions.add(from.id(), TransactionType.TRANSFER_OUT, amountCents, to.number(), finalTitle,
                    fromAfter.balanceCents());
            transactions.add(to.id(), TransactionType.TRANSFER_IN, amountCents, from.number(), finalTitle,
                    toAfter.balanceCents());
            return fromAfter;
        });
    }

    public List<Transaction> history(long userId, long accountId, int limit) throws SQLException {
        owned(userId, accountId);
        return transactions.findByAccount(accountId, limit);
    }

    public void closeAccount(long userId, long accountId) throws SQLException {
        db.inTransaction(() -> {
            Account acc = owned(userId, accountId);
            if (acc.balanceCents() != 0) {
                throw new BankException("Konto można zamknąć tylko przy saldzie 0. Najpierw wypłać lub przelej środki.");
            }
            accounts.delete(acc.id());
            return null;
        });
    }

    /** Pobiera konto i sprawdza, że należy do zalogowanego użytkownika. */
    private Account owned(long userId, long accountId) throws SQLException {
        return accounts.findById(accountId)
                .filter(a -> a.userId() == userId)
                .orElseThrow(() -> new BankException("Nie znaleziono konta."));
    }
}
