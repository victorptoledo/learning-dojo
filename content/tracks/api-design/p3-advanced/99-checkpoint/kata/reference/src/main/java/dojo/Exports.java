package dojo;

import dojo.ApiExceptions.ExportNotFoundException;
import dojo.Exporter.ExportFailedException;
import dojo.Partners.Partner;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Exports as long-running operations: created running, finished by a background job. */
@Component
public class Exports {
    record ExportError(String type, String title, String detail) {}

    record Export(String id, Partner partner, String dataset, String format, String status,
                  String resultUrl, ExportError error) {
        Export succeeded(String url) {
            return new Export(id, partner, dataset, format, "SUCCEEDED", url, null);
        }

        Export failed(String detail) {
            return new Export(id, partner, dataset, format, "FAILED", null,
                    new ExportError(ApiProblems.TYPES + "export-failed", "Export failed", detail));
        }
    }

    private final Exporter exporter;
    private final BackgroundJobs jobs;
    private final WebhookPublisher webhooks;
    private final Map<String, Export> exports = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public Exports(Exporter exporter, BackgroundJobs jobs, WebhookPublisher webhooks) {
        this.exporter = exporter;
        this.jobs = jobs;
        this.webhooks = webhooks;
    }

    Export start(Partner partner, String dataset, String format) {
        String id = "exp_" + ids.incrementAndGet();
        Export export = new Export(id, partner, dataset, format, "RUNNING", null, null);
        exports.put(id, export);
        jobs.submit(() -> finish(id));
        return export;
    }

    /** The partner's own export; someone else's is as unknown as one that does not exist. */
    Export find(Partner partner, String id) {
        Export export = exports.get(id);
        if (export == null || !export.partner().id().equals(partner.id())) {
            throw new ExportNotFoundException(id);
        }
        return export;
    }

    private void finish(String id) {
        Export running = exports.get(id);
        Export done;
        try {
            done = running.succeeded(exporter.run(id, running.dataset(), running.format()));
        } catch (ExportFailedException e) {
            done = running.failed(e.getMessage());
        }
        exports.put(id, done);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportId", id);
        data.put("status", done.status());
        if (done.resultUrl() != null) {
            data.put("resultUrl", done.resultUrl());
        } else {
            data.put("detail", done.error().detail());
        }
        webhooks.publish(done.partner(), done.resultUrl() != null ? "export.succeeded" : "export.failed", data);
    }
}
