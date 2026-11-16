package dojo.rentals.booking.domain;

import dojo.legacy.crm.CrmClient;
import dojo.rentals.billing.internal.Ledger;
import java.util.Map;

/** Takes bookings. It speaks the CRM's language and writes into billing's ledger. */
public class BookingService {
    private final CrmClient crm;
    private final Ledger ledger;

    public BookingService(CrmClient crm, Ledger ledger) {
        this.crm = crm;
        this.ledger = ledger;
    }

    public Booking book(String bookingId, String customerId, String vehicle, int days, long dailyRateCents) {
        if (days < 1) {
            throw new IllegalArgumentException("bad number of days " + days);
        }
        Map<String, String> record = crm.fetchCustomer(customerId);
        if (record == null) {
            throw new IllegalArgumentException("unknown customer " + customerId);
        }
        if (record.get("STAT_CD").equals("S")) {
            throw new BookingRefusedException("customer is suspended");
        }
        if (record.get("STAT_CD").equals("X")) {
            throw new BookingRefusedException("customer is closed");
        }
        long price = days * dailyRateCents;
        if (record.get("SEG").equals("02")) {
            price = price * 9 / 10;
        }
        if (price > Long.parseLong(record.get("CRDT_LMT"))) {
            throw new BookingRefusedException("over credit limit");
        }
        String chargeId = ledger.post(customerId, bookingId, price);
        return new Booking(bookingId, customerId, vehicle, days, price, chargeId);
    }
}
