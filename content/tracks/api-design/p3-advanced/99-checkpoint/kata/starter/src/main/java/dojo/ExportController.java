package dojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The first version, written in a hurry: the key travels in the query string, the export
 * runs inside the request (minutes, in production), and any partner can read any export.
 */
@RestController
@RequestMapping("/exports")
public class ExportController {
    record ExportRequest(@NotNull @Pattern(regexp = "orders|customers|invoices") String dataset,
                         @NotNull @Pattern(regexp = "csv|ndjson") String format) {}

    record ExportView(String id, String dataset, String format, String status, String resultUrl) {}

    private final Partners partners;
    private final Exporter exporter;
    private final Map<String, ExportView> exports = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public ExportController(Partners partners, Exporter exporter) {
        this.partners = partners;
        this.exporter = exporter;
    }

    @PostMapping
    public ExportView create(@RequestParam String apiKey, @Valid @RequestBody ExportRequest request) {
        partners.authenticate(apiKey).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String id = "exp_" + ids.incrementAndGet();
        String resultUrl = exporter.run(id, request.dataset(), request.format());
        ExportView export = new ExportView(id, request.dataset(), request.format(), "SUCCEEDED", resultUrl);
        exports.put(id, export);
        return export;
    }

    @GetMapping("/{id}")
    public ExportView get(@PathVariable String id) {
        ExportView export = exports.get(id);
        if (export == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return export;
    }
}
