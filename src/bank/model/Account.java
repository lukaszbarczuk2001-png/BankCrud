package bank.model;

/** Saldo trzymane w groszach (long), żeby uniknąć błędów zaokrągleń. */
public record Account(long id, String number, long userId, long balanceCents, String createdAt) {
}
