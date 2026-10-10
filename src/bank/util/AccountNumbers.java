package bank.util;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Numery kont w formacie polskiego NRB (26 cyfr) z poprawną sumą kontrolną IBAN,
 * wyświetlane jako XX XXXX XXXX XXXX XXXX XXXX XXXX.
 */
public final class AccountNumbers {

    /** Wymyślony 8-cyfrowy "numer rozliczeniowy" naszego banku. */
    private static final String BANK_CODE = "10102030";
    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);
    private static final String PL_DIGITS = "2521"; // P=25, L=21 wg IBAN
    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountNumbers() {
    }

    public static String generate() {
        StringBuilder bban = new StringBuilder(BANK_CODE);
        for (int i = 0; i < 16; i++) {
            bban.append(RANDOM.nextInt(10));
        }
        int mod = new BigInteger(bban + PL_DIGITS + "00").mod(NINETY_SEVEN).intValue();
        return String.format("%02d", 98 - mod) + bban;
    }

    /** Usuwa spacje, myślniki i ewentualny prefiks PL. */
    public static String normalize(String input) {
        String s = input.replaceAll("[\\s-]", "").toUpperCase();
        return s.startsWith("PL") ? s.substring(2) : s;
    }

    public static boolean isValid(String number) {
        if (!number.matches("\\d{26}")) {
            return false;
        }
        String rearranged = number.substring(2) + PL_DIGITS + number.substring(0, 2);
        return new BigInteger(rearranged).mod(NINETY_SEVEN).intValue() == 1;
    }

    /** 26 cyfr -> "XX XXXX XXXX XXXX XXXX XXXX XXXX" */
    public static String pretty(String number) {
        StringBuilder sb = new StringBuilder(number.substring(0, 2));
        for (int i = 2; i < number.length(); i += 4) {
            sb.append(' ').append(number, i, Math.min(i + 4, number.length()));
        }
        return sb.toString();
    }
}
