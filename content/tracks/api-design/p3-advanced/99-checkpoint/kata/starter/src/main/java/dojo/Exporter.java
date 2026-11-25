package dojo;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Builds the export file. Written for you. In production it takes minutes, which is why it
 * must not run inside a request; the tests count how often it runs. The invoices warehouse
 * is offline, so every invoices export fails.
 */
@Component
public class Exporter {
    private final AtomicInteger runs = new AtomicInteger();

    public static final class ExportFailedException extends RuntimeException {
        public ExportFailedException(String message) {
            super(message);
        }
    }

    /** Builds the file for this export and returns where it can be downloaded. */
    public String run(String exportId, String dataset, String format) {
        runs.incrementAndGet();
        if ("invoices".equals(dataset)) {
            throw new ExportFailedException("the invoices warehouse is offline");
        }
        return "https://files.dojo.example/exports/" + exportId + "." + format;
    }

    public int runs() {
        return runs.get();
    }
}
