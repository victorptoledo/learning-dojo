package dojo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** The customers, in memory. Written for you. Seeded with customers 1 to 3. */
@Component
public class CustomerStore {
    public static final class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(long id) {
            super("customer " + id + " does not exist");
        }
    }

    private final Map<Long, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public CustomerStore() {
        create("Ada", "Lovelace", List.of("ada@example.com"), "+44 20 7946 0001", CustomerStatus.ACTIVE, "GB");
        create("Linus", "Torvalds", List.of("linus@example.org", "lt@example.com"), "+358 9 0000 0002",
                CustomerStatus.INACTIVE, "FI");
        create("Grace", "Hopper", List.of("grace@example.mil"), null, CustomerStatus.SUSPENDED, "US");
    }

    public Customer get(long id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            throw new CustomerNotFoundException(id);
        }
        return customer;
    }

    /** A new customer, version 1. The country may be null (unknown). */
    public Customer create(String givenName, String familyName, List<String> emails, String phoneNumber,
            CustomerStatus status, String country) {
        long id = nextId.getAndIncrement();
        Customer customer = new Customer(id, givenName, familyName, List.copyOf(emails), phoneNumber, status,
                country, 1);
        customers.put(id, customer);
        return customer;
    }
}
