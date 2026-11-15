package dojo;

/** Proof of an accepted payment: the provider's reference and the amount in cents. */
public record Receipt(String reference, long cents) {}
