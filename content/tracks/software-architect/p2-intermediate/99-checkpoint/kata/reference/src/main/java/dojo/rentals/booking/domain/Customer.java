package dojo.rentals.booking.domain;

/** A customer in booking's own words. */
public record Customer(String id, String displayName, Standing standing, long creditLimitCents, boolean business) {}
