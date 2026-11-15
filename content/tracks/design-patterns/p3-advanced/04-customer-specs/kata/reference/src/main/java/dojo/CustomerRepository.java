package dojo;

import static dojo.CustomerSpec.active;
import static dojo.CustomerSpec.inCountry;
import static dojo.CustomerSpec.minOrders;

import java.util.List;

/** Customers kept in memory, queried by specification. The old finders are one-line specs. */
public class CustomerRepository {
    private final List<Customer> customers;

    public CustomerRepository(List<Customer> customers) {
        this.customers = List.copyOf(customers);
    }

    public List<Customer> findAll(CustomerSpec spec) {
        return customers.stream().filter(spec::isSatisfiedBy).toList();
    }

    public long count(CustomerSpec spec) {
        return customers.stream().filter(spec::isSatisfiedBy).count();
    }

    public List<Customer> findActive() {
        return findAll(active());
    }

    public List<Customer> findActiveInCountry(String country) {
        return findAll(active().and(inCountry(country)));
    }

    public List<Customer> findLoyalInCountry(String country, int minOrders) {
        return findAll(active().and(inCountry(country)).and(minOrders(minOrders)));
    }

    public List<Customer> findDormant() {
        return findAll(active().not().or(minOrders(1).not()));
    }

    public long countActiveOutside(String country) {
        return count(active().and(inCountry(country).not()));
    }
}
