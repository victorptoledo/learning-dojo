package dojo.rentals.booking.domain;

import java.util.Optional;

/** Port: where booking finds its customers. */
public interface CustomerDirectory {
    Optional<Customer> find(String customerId);
}
