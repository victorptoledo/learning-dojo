package dojo.rentals.booking.domain;

/** Do not change it. */
public record Booking(String bookingId, String customerId, String vehicle, int days, long priceCents,
                      String chargeId) {}
