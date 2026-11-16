package dojo;

/** A withdrawal that went through. */
public record Withdrawal(long id, String accountId, long amountCents, String reference, long balanceAfterCents) {}
