package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.legacy.crm.CrmClient;
import dojo.rentals.App;
import dojo.rentals.Rentals;
import dojo.rentals.booking.acl.CrmCustomerDirectory;
import dojo.rentals.booking.domain.Booking;
import dojo.rentals.booking.domain.Customer;
import dojo.rentals.booking.domain.Standing;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RentalsTest {
    private static CrmClient crm() {
        CrmClient crm = new CrmClient();
        crm.load("C-1", Map.of("CUST_NM", "LOVELACE, ADA", "STAT_CD", "A", "CRDT_LMT", "000050000", "SEG", "01"));
        return crm;
    }

    @Test
    void theCrmRecordIsTranslated() {
        assertEquals(new Customer("C-1", "Ada Lovelace", Standing.GOOD, 50000, false),
                new CrmCustomerDirectory(crm()).find("C-1").orElseThrow());
    }

    @Test
    void aBookingIsCharged() {
        Rentals rentals = App.create(crm());
        assertEquals(new Booking("B-1", "C-1", "VW Golf", 3, 15000, "CH-1"),
                rentals.booking().book("B-1", "C-1", "VW Golf", 3, 5000));
        assertEquals(1, rentals.billing().charges().size());
    }
}
