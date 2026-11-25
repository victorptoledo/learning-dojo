package dojo;

/** The balance converted to {@code currency}; {@code stale} when the last known rate was used. */
public record BalanceView(String currency, long amountCents, boolean stale) {}
