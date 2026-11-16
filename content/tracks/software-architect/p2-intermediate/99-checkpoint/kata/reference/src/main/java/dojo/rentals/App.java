package dojo.rentals;

import dojo.legacy.crm.CrmClient;
import dojo.rentals.billing.api.Billing;
import dojo.rentals.billing.internal.BillingService;
import dojo.rentals.booking.acl.CrmCustomerDirectory;
import dojo.rentals.booking.domain.BookingService;

/** The composition root. */
public final class App {
    private App() {}

    public static Rentals create(CrmClient crm) {
        Billing billing = new BillingService();
        return new Rentals(new BookingService(new CrmCustomerDirectory(crm), billing), billing);
    }
}
