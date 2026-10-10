package bank.model;

public enum TransactionType {
    DEPOSIT("Wpłata"),
    WITHDRAWAL("Wypłata"),
    TRANSFER_OUT("Przelew wychodzący"),
    TRANSFER_IN("Przelew przychodzący");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Czy operacja zmniejsza saldo. */
    public boolean isDebit() {
        return this == WITHDRAWAL || this == TRANSFER_OUT;
    }
}
