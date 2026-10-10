# BankCrud

Prosta konsolowa aplikacja bankowa w Javie z bazą SQLite.

## Funkcje
- rejestracja (login, imię i nazwisko, e-mail, hasło), logowanie i wylogowanie
- profil: podgląd, edycja danych, zmiana hasła, usunięcie profilu
- konta: otwieranie (do 5 kont), lista z saldami, zamykanie (przy saldzie 0)
- operacje: wpłata, wypłata, przelew na inne konto w banku, historia operacji

Hasła są haszowane algorytmem PBKDF2, a kwoty przechowywane w groszach.
Przelew wykonuje się w jednej transakcji bazodanowej, a numery kont mają
format NRB (26 cyfr) z poprawną sumą kontrolną.

## Uruchomienie w IntelliJ IDEA
1. Otwórz projekt. Gdy pojawi się „Maven build scripts found”, kliknij **Load Maven Project**.
   Jeśli komunikat się nie pojawi, kliknij prawym przyciskiem `pom.xml` i wybierz
   **Add as Maven Project**.
2. Poczekaj, aż IntelliJ pobierze sterownik `sqlite-jdbc`.
3. Otwórz `src/Main.java` i kliknij zieloną strzałkę obok `main`.

Baza zapisuje się w pliku `bank.db` w folderze projektu. Żeby zacząć od zera,
usuń ten plik.

## Podgląd bazy
- IntelliJ Ultimate: zakładka **Database** → `+` → Data Source → SQLite → plik `bank.db`
- albo darmowy program [DB Browser for SQLite](https://sqlitebrowser.org/)

## Struktura
```
src/
  Main.java                  punkt startowy
  bank/db/Database           połączenie, tworzenie tabel, transakcje
  bank/model/                rekordy: User, Account, Transaction
  bank/repository/           zapytania SQL (CRUD)
  bank/service/              logika: walidacja, rejestracja, przelewy
  bank/ui/ConsoleApp         menu w konsoli
  bank/util/                 hasła, kwoty, numery kont
```

## Tabele
| Tabela         | Najważniejsze kolumny |
|----------------|-----------------------|
| `users`        | id, username, full_name, email, password_hash, created_at |
| `accounts`     | id, number, user_id → users, balance_cents, created_at |
| `transactions` | id, account_id → accounts, type, amount_cents, counterparty, title, balance_after_cents, created_at |
