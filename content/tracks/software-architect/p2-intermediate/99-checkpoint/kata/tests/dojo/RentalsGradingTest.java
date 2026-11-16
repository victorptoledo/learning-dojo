package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.legacy.crm.CrmClient;
import dojo.rentals.App;
import dojo.rentals.Rentals;
import dojo.rentals.billing.api.Billing;
import dojo.rentals.billing.api.ChargeReceipt;
import dojo.rentals.billing.api.ChargeRequest;
import dojo.rentals.billing.internal.BillingService;
import dojo.rentals.booking.acl.CrmCustomerDirectory;
import dojo.rentals.booking.domain.Booking;
import dojo.rentals.booking.domain.BookingRefusedException;
import dojo.rentals.booking.domain.BookingService;
import dojo.rentals.booking.domain.Customer;
import dojo.rentals.booking.domain.Standing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RentalsGradingTest {
    private final CrmClient crm = new CrmClient();
    private final Rentals rentals = App.create(crm);

    RentalsGradingTest() {
        crm.load("C-1", Map.of("CUST_NM", "LOVELACE, ADA", "STAT_CD", "A", "CRDT_LMT", "000050000", "SEG", "01"));
        crm.load("C-2", Map.of("CUST_NM", "HOPPER, GRACE BREWSTER", "STAT_CD", "A", "CRDT_LMT", "000100000", "SEG", "02"));
        crm.load("C-3", Map.of("CUST_NM", "BABBAGE, CHARLES", "STAT_CD", "S", "CRDT_LMT", "000100000", "SEG", "01"));
        crm.load("C-4", Map.of("CUST_NM", "TURING, ALAN", "STAT_CD", "X", "CRDT_LMT", "000100000", "SEG", "01"));
        crm.load("C-5", Map.of("CUST_NM", "KNUTH, DONALD", "STAT_CD", "P", "CRDT_LMT", "000100000", "SEG", "01"));
    }

    @Test
    void theAclTranslatesNamesStandingsLimitsAndSegments() {
        CrmCustomerDirectory directory = new CrmCustomerDirectory(crm);
        assertEquals(Optional.of(new Customer("C-1", "Ada Lovelace", Standing.GOOD, 50000, false)), directory.find("C-1"));
        assertEquals(Optional.of(new Customer("C-2", "Grace Brewster Hopper", Standing.GOOD, 100000, true)), directory.find("C-2"));
        assertEquals(Standing.SUSPENDED, directory.find("C-3").orElseThrow().standing());
        assertEquals(Standing.CLOSED, directory.find("C-4").orElseThrow().standing());
        assertEquals(Optional.empty(), directory.find("C-9"));
    }

    @Test
    void anUnknownCrmStatusIsAnError() {
        assertThrows(IllegalStateException.class, () -> new CrmCustomerDirectory(crm).find("C-5"));
    }

    @Test
    void aRetailBookingIsChargedInFull() {
        Booking booking = rentals.booking().book("B-1", "C-1", "VW Golf", 3, 5000);
        assertEquals(new Booking("B-1", "C-1", "VW Golf", 3, 15000, "CH-1"), booking);
        assertEquals(List.of(new ChargeReceipt("CH-1", "C-1", "B-1", 15000)), rentals.billing().charges());
    }

    @Test
    void businessCustomersPayNinetyPercentRoundedDown() {
        Booking booking = rentals.booking().book("B-2", "C-2", "Van", 3, 3333);
        assertEquals(8999, booking.priceCents());
    }

    @Test
    void theCreditLimitIsCheckedAfterTheDiscount() {
        rentals.booking().book("B-3", "C-1", "Car", 10, 5000);
        assertThrows(BookingRefusedException.class, () -> rentals.booking().book("B-4", "C-1", "Car", 10, 5001));
        rentals.booking().book("B-5", "C-2", "Van", 10, 11111);
        BookingRefusedException over = assertThrows(BookingRefusedException.class,
                () -> rentals.booking().book("B-6", "C-2", "Van", 10, 11112));
        assertEquals("over credit limit", over.getMessage());
        assertEquals(2, rentals.billing().charges().size());
    }

    @Test
    void suspendedAndClosedCustomersAreRefused() {
        assertEquals("customer is suspended", assertThrows(BookingRefusedException.class,
                () -> rentals.booking().book("B-7", "C-3", "Car", 1, 100)).getMessage());
        assertEquals("customer is closed", assertThrows(BookingRefusedException.class,
                () -> rentals.booking().book("B-8", "C-4", "Car", 1, 100)).getMessage());
        assertEquals(List.of(), rentals.billing().charges());
    }

    @Test
    void badInputIsRejected() {
        assertEquals("unknown customer C-9", assertThrows(IllegalArgumentException.class,
                () -> rentals.booking().book("B-9", "C-9", "Car", 1, 100)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> rentals.booking().book("B-9", "C-1", "Car", 0, 100));
        assertEquals(List.of(), rentals.billing().charges());
    }

    @Test
    void bookingWorksWithAnyDirectoryAndBilling() {
        List<ChargeRequest> requests = new ArrayList<>();
        Billing billing = new Billing() {
            @Override
            public ChargeReceipt charge(ChargeRequest request) {
                requests.add(request);
                return new ChargeReceipt("X-1", request.customerId(), request.reference(), request.amountCents());
            }

            @Override
            public List<ChargeReceipt> charges() {
                return List.of();
            }
        };
        BookingService service = new BookingService(
                id -> Optional.of(new Customer(id, "Someone", Standing.GOOD, 1000, false)), billing);

        assertEquals("X-1", service.book("B-1", "Z-1", "Bike", 2, 300).chargeId());
        assertEquals(List.of(new ChargeRequest("Z-1", "B-1", 600)), requests);
    }

    @Test
    void billingNumbersItsCharges() {
        BillingService billing = new BillingService();
        assertEquals(new ChargeReceipt("CH-1", "a", "r1", 10), billing.charge(new ChargeRequest("a", "r1", 10)));
        assertEquals(new ChargeReceipt("CH-2", "b", "r2", 20), billing.charge(new ChargeRequest("b", "r2", 20)));
        assertTrue(billing.charges().size() == 2);
    }
}
