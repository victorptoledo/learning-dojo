package dojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Customers kept in memory. Every question the business asks became one more finder, each
 * repeating the same conditions in a slightly different combination.
 */
public class CustomerRepository {
    private final List<Customer> customers;

    public CustomerRepository(List<Customer> customers) {
        this.customers = List.copyOf(customers);
    }

    public List<Customer> findActive() {
        List<Customer> found = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.active()) found.add(customer);
        }
        return found;
    }

    public List<Customer> findActiveInCountry(String country) {
        List<Customer> found = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.active() && customer.country().equals(country)) found.add(customer);
        }
        return found;
    }

    public List<Customer> findLoyalInCountry(String country, int minOrders) {
        List<Customer> found = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.active() && customer.country().equals(country) && customer.orders() >= minOrders) {
                found.add(customer);
            }
        }
        return found;
    }

    public List<Customer> findDormant() {
        List<Customer> found = new ArrayList<>();
        for (Customer customer : customers) {
            if (!customer.active() || customer.orders() < 1) found.add(customer);
        }
        return found;
    }

    public long countActiveOutside(String country) {
        long count = 0;
        for (Customer customer : customers) {
            if (customer.active() && !customer.country().equals(country)) count++;
        }
        return count;
    }
}
