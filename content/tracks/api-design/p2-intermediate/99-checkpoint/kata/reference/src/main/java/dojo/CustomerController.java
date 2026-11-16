package dojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Version 1, frozen: the representation the mobile app knows, computed from the new model, and
 * marked deprecated with a sunset date and a link to its successor.
 */
@RestController
@RequestMapping("/customers")
public class CustomerController {
    /** RFC 9745: 2026-11-01T00:00:00Z as a structured-field date. */
    static final String DEPRECATION = "@1793491200";
    /** RFC 8594: after this date version 1 may stop answering. */
    static final String SUNSET = "Tue, 01 Jun 2027 00:00:00 GMT";
    static final String MIGRATION_GUIDE = "<https://api.dojo.example/docs/migrate-to-v2>; rel=\"deprecation\"";

    private final CustomerStore store;

    public CustomerController(CustomerStore store) {
        this.store = store;
    }

    /** What version 1 clients read. */
    public record CustomerV1(long id, String name, String email, String phone, String status) {
        static CustomerV1 of(Customer customer) {
            String name = (customer.givenName() + " " + customer.familyName()).strip();
            // Old clients have a closed enum: the new SUSPENDED is, for them, INACTIVE.
            String status = customer.status() == CustomerStatus.ACTIVE ? "ACTIVE" : "INACTIVE";
            return new CustomerV1(customer.id(), name, customer.emails().getFirst(), customer.phoneNumber(), status);
        }
    }

    /** What version 1 clients send. */
    public record SignupV1(@NotBlank String name, @NotBlank @Email String email, String phone) {}

    static String etag(Customer customer) {
        return "\"v1-" + customer.version() + "\"";
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerV1> get(@PathVariable long id) {
        Customer customer = store.get(id);
        return deprecated(ResponseEntity.ok(), "</v2/customers/" + id + ">; rel=\"successor-version\"")
                .eTag(etag(customer))
                .body(CustomerV1.of(customer));
    }

    @PostMapping
    public ResponseEntity<CustomerV1> create(@Valid @RequestBody SignupV1 request) {
        String name = request.name().strip();
        int space = name.indexOf(' ');
        String given = space < 0 ? name : name.substring(0, space);
        String family = space < 0 ? "" : name.substring(space + 1).strip();
        Customer customer = store.create(given, family, List.of(request.email()), request.phone(),
                CustomerStatus.ACTIVE, null);
        return deprecated(ResponseEntity.created(URI.create("/customers/" + customer.id())),
                        "</v2/customers/" + customer.id() + ">; rel=\"successor-version\"")
                .eTag(etag(customer))
                .body(CustomerV1.of(customer));
    }

    private static ResponseEntity.BodyBuilder deprecated(ResponseEntity.BodyBuilder builder, String successor) {
        return builder
                .header("Deprecation", DEPRECATION)
                .header("Sunset", SUNSET)
                .header(HttpHeaders.LINK, successor + ", " + MIGRATION_GUIDE);
    }
}
