package dojo.rentals.billing.api;

/** Do not change it. */
public record ChargeReceipt(String chargeId, String customerId, String reference, long amountCents) {}
