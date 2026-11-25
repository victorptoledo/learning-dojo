package dojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import dojo.Exports.Export;
import dojo.Exports.ExportError;
import dojo.Partners.Partner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exports for partners: started with 202, polled on their status resource. */
@RestController
@RequestMapping("/exports")
public class ExportController {
    static final String POLL_SECONDS = "5";

    record ExportRequest(@NotNull @Pattern(regexp = "orders|customers|invoices") String dataset,
                         @NotNull @Pattern(regexp = "csv|ndjson") String format) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ExportView(String id, String dataset, String format, String status, String resultUrl, ExportError error) {
        static ExportView of(Export export) {
            return new ExportView(export.id(), export.dataset(), export.format(), export.status(),
                    export.resultUrl(), export.error());
        }
    }

    private final Exports exports;

    public ExportController(Exports exports) {
        this.exports = exports;
    }

    @PostMapping
    @RequiresScope("exports:write")
    public ResponseEntity<ExportView> create(@RequestAttribute(PartnerAccess.PARTNER) Partner partner,
                                             @Valid @RequestBody ExportRequest request) {
        Export export = exports.start(partner, request.dataset(), request.format());
        return ResponseEntity.accepted()
                .location(URI.create("/exports/" + export.id()))
                .header(HttpHeaders.RETRY_AFTER, POLL_SECONDS)
                .body(ExportView.of(export));
    }

    @GetMapping("/{id}")
    @RequiresScope("exports:read")
    public ResponseEntity<ExportView> get(@RequestAttribute(PartnerAccess.PARTNER) Partner partner,
                                          @PathVariable String id) {
        Export export = exports.find(partner, id);
        ResponseEntity.BodyBuilder answer = ResponseEntity.ok();
        if ("RUNNING".equals(export.status())) {
            answer.header(HttpHeaders.RETRY_AFTER, POLL_SECONDS);
        }
        return answer.body(ExportView.of(export));
    }
}
