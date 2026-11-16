package dojo;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Last week's release: the new customer model, served straight from /customers. It broke the
 * mobile app, which still speaks the first version of the API (see LegacyClientContractTest).
 */
@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerStore store;

    public CustomerController(CustomerStore store) {
        this.store = store;
    }

    public record NewCustomer(String givenName, String familyName, List<String> emails, String phoneNumber,
            String country) {}

    @GetMapping("/{id}")
    public Customer get(@PathVariable long id) {
        return store.get(id);
    }

    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody NewCustomer request) {
        Customer customer = store.create(request.givenName(), request.familyName(), request.emails(),
                request.phoneNumber(), CustomerStatus.ACTIVE, request.country());
        return ResponseEntity.created(URI.create("/customers/" + customer.id())).body(customer);
    }
}
