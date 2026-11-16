package dojo.shop.billing;

/** What billing needs to write an invoice, in billing's own words. */
public record InvoiceRequest(String orderId, String customer, long netCents) {}
