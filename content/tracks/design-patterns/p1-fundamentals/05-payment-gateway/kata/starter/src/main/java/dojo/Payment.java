package dojo;

/** A payment to take: the customer's account, the amount in cents and the ISO currency. */
public record Payment(String account, long cents, String currency) {}
