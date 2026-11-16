package dojo.rentals;

import dojo.legacy.crm.CrmClient;
import dojo.rentals.billing.internal.Ledger;
import dojo.rentals.booking.domain.BookingService;

/** The composition root. */
public final class App {
    private App() {}

    public static BookingService create(CrmClient crm) {
        return new BookingService(crm, new Ledger());
    }
}
