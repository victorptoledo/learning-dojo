package dojo.billing;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {
    private final Billing billing;

    public InvoiceController(Billing billing) {
        this.billing = billing;
    }

    @PostMapping
    public ResponseEntity<InvoiceView> issue(@Valid @RequestBody NewInvoice request) {
        InvoiceView invoice = billing.issue(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{number}")
                .buildAndExpand(invoice.number()).toUri();
        return ResponseEntity.created(location).body(invoice);
    }

    @GetMapping("/{number}")
    public InvoiceView get(@PathVariable String number) {
        return billing.find(number);
    }

    @GetMapping(path = "/{number}/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public String text(@PathVariable String number) {
        return billing.text(number);
    }
}
