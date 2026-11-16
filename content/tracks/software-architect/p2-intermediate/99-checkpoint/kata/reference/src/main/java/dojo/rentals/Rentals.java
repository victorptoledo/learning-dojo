package dojo.rentals;

import dojo.rentals.billing.api.Billing;
import dojo.rentals.booking.domain.BookingService;

/** The wired rental. */
public record Rentals(BookingService booking, Billing billing) {}
