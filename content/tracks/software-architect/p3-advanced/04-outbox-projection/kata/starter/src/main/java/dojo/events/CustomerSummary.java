package dojo.events;

/** One row of the read model. Do not change it. */
public record CustomerSummary(String customerId, int openOrders, long openAmountCents) {}
