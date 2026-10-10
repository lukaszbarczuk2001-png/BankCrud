package bank.model;

public record Transaction(long id, long accountId, TransactionType type, long amountCents,
                          String counterparty, String title, long balanceAfterCents, String createdAt) {
}
