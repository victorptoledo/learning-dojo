package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.rentals.booking.domain.Customer;
import dojo.rentals.booking.domain.CustomerDirectory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class RentalsArchitectureGradingTest {
    private static final Map<String, Set<String>> RENTALS = GradingGraph.scan("dojo.rentals");

    private static List<String> uses(Predicate<String> from, Predicate<String> forbidden) {
        return RENTALS.entrySet().stream()
                .filter(entry -> from.test(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream().filter(forbidden).map(to -> entry.getKey() + " -> " + to))
                .toList();
    }

    @Test
    void onlyTheAclAndTheRootUseTheLegacyCrm() {
        assertTrue(RENTALS.containsKey("dojo.rentals.booking.domain.BookingService"), "the scan found no rental classes");
        assertEquals(List.of(), uses(
                name -> !name.startsWith("dojo.rentals.booking.acl.") && !name.equals("dojo.rentals.App"),
                to -> to.startsWith("dojo.legacy.")));
    }

    @Test
    void theBookingDomainKnowsNeitherTheAclNorBillingsInsides() {
        assertEquals(List.of(), uses(name -> name.startsWith("dojo.rentals.booking.domain."),
                to -> to.startsWith("dojo.rentals.booking.acl.")
                        || to.startsWith("dojo.rentals.billing.") && !to.startsWith("dojo.rentals.billing.api.")));
    }

    @Test
    void billingKnowsNothingOfBooking() {
        assertEquals(List.of(), uses(name -> name.startsWith("dojo.rentals.billing."),
                to -> to.startsWith("dojo.rentals.booking.")));
    }

    @Test
    void bookingHasItsOwnCustomerModel() {
        assertTrue(Customer.class.isRecord(), "Customer should be a record");
        assertEquals(List.of("String id", "String displayName", "Standing standing", "long creditLimitCents", "boolean business"),
                Arrays.stream(Customer.class.getRecordComponents())
                        .map(c -> c.getType().getSimpleName() + " " + c.getName()).toList());
        assertTrue(CustomerDirectory.class.isInterface(), "CustomerDirectory should be an interface");
    }
}
