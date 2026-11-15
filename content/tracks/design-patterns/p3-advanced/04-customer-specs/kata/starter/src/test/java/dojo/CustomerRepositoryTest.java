package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Characterisation tests for the finders: they pass before and after the refactoring. Keep
 * them green while you move the code. (CustomerQueries changes its API; see README.md.)
 */
class CustomerRepositoryTest {
    static final Customer ANA = new Customer("ana", "PT", true, 12);
    static final Customer BO = new Customer("bo", "PT", true, 0);
    static final Customer CY = new Customer("cy", "ES", true, 3);
    static final Customer DEE = new Customer("dee", "PT", false, 40);

    private final CustomerRepository repository = new CustomerRepository(List.of(ANA, BO, CY, DEE));

    @Test
    void finders_keep_insertion_order() {
        assertEquals(List.of(ANA, BO, CY), repository.findActive());
        assertEquals(List.of(ANA, BO), repository.findActiveInCountry("PT"));
        assertEquals(List.of(ANA), repository.findLoyalInCountry("PT", 10));
    }

    @Test
    void dormant_customers_are_inactive_or_never_ordered() {
        assertEquals(List.of(BO, DEE), repository.findDormant());
        assertEquals(1, repository.countActiveOutside("PT"));
    }
}
