package dojo.rentals.booking.domain;

import dojo.rentals.billing.api.Billing;
import dojo.rentals.billing.api.ChargeReceipt;
import dojo.rentals.billing.api.ChargeRequest;
import java.util.Locale;

/** Takes bookings, in booking's own language. */
public class BookingService {
    private final CustomerDirectory customers;
    private final Billing billing;

    public BookingService(CustomerDirectory customers, Billing billing) {
        this.customers = customers;
        this.billing = billing;
    }

    public Booking book(String bookingId, String customerId, String vehicle, int days, long dailyRateCents) {
        if (days < 1) {
            throw new IllegalArgumentException("bad number of days " + days);
        }
        Customer customer = customers.find(customerId)
                .orElseThrow(() -> new IllegalArgumentException("unknown customer " + customerId));
        if (customer.standing() != Standing.GOOD) {
            throw new BookingRefusedException("customer is " + customer.standing().name().toLowerCase(Locale.ROOT));
        }
        long price = days * dailyRateCents;
        if (customer.business()) {
            price = price * 9 / 10;
        }
        if (price > customer.creditLimitCents()) {
            throw new BookingRefusedException("over credit limit");
        }
        ChargeReceipt receipt = billing.charge(new ChargeRequest(customerId, bookingId, price));
        return new Booking(bookingId, customerId, vehicle, days, price, receipt.chargeId());
    }
}
