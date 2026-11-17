package dojo.wallet;

/** A row of the balances report, selected straight from JPQL. */
public record BalanceRow(String owner, long balanceCents, long entries) {
}
