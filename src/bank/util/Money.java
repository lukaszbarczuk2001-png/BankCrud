package bank.util;

import bank.BankException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/** Zamiana kwot tekst <-> grosze. */
public final class Money {

    private static final long MAX_CENTS = 1_000_000_000_00L; // 1 mld zł na jedną operację

    private Money() {
    }

    /** "123,45" albo "123.45" -> 12345 groszy. */
    public static long parse(String text) {
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim().replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new BankException("Nieprawidłowa kwota: " + text);
        }
        if (value.signum() <= 0) {
            throw new BankException("Kwota musi być większa od zera.");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new BankException("Kwota może mieć najwyżej 2 miejsca po przecinku.");
        }
        BigDecimal cents = value.movePointRight(2);
        if (cents.compareTo(BigDecimal.valueOf(MAX_CENTS)) > 0) {
            throw new BankException("Kwota jest za duża.");
        }
        return cents.longValueExact();
    }

    /** 12345 -> "123,45 PLN" */
    public static String format(long cents) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(BigDecimal.valueOf(cents, 2)) + " PLN";
    }
}
