package dojo.rentals.billing.api;

/** Do not change it. */
public record ChargeRequest(String customerId, String reference, long amountCents) {}
