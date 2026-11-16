package dojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Version 2: the new model, under its own path. Same customers, another representation. */
@RestController
@RequestMapping("/v2/customers")
public class CustomerV2Controller {
    private final CustomerStore store;

    public CustomerV2Controller(CustomerStore store) {
        this.store = store;
    }

    public record CustomerV2(long id, String givenName, String familyName, List<String> emails, String phoneNumber,
            CustomerStatus status, String country) {
        static CustomerV2 of(Customer c) {
            return new CustomerV2(c.id(), c.givenName(), c.familyName(), c.emails(), c.phoneNumber(), c.status(),
                    c.country());
        }
    }

    public record SignupV2(
            @NotBlank String givenName,
            @NotNull String familyName,
            @NotEmpty List<@NotBlank @Email String> emails,
            String phoneNumber,
            @NotNull @Pattern(regexp = "[A-Z]{2}") String country) {}

    static String etag(Customer customer) {
        return "\"v2-" + customer.version() + "\"";
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerV2> get(@PathVariable long id) {
        Customer customer = store.get(id);
        return ResponseEntity.ok().eTag(etag(customer)).body(CustomerV2.of(customer));
    }

    @PostMapping
    public ResponseEntity<CustomerV2> create(@Valid @RequestBody SignupV2 request) {
        Customer customer = store.create(request.givenName(), request.familyName(), request.emails(),
                request.phoneNumber(), CustomerStatus.ACTIVE, request.country());
        return ResponseEntity.created(URI.create("/v2/customers/" + customer.id()))
                .eTag(etag(customer))
                .body(CustomerV2.of(customer));
    }
}
