package dojo;

/** A customer: name, ISO country code, whether the account is active and how many orders it placed. */
public record Customer(String name, String country, boolean active, int orders) {}
