package dojo;

import dojo.DocumentExceptions.DocumentNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * The documents, in memory. Written for you. Seeded with "readme", "changelog" and "license"
 * (version 1, updated 2026-04-01T12:00:00Z). Every change raises the version by one and moves
 * updatedAt forward by at least a second.
 */
@Component
public class DocumentStore {
    private static final Instant SEEDED = Instant.parse("2026-04-01T12:00:00Z");

    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public DocumentStore() {
        for (String id : new String[] {"readme", "changelog", "license"}) {
            documents.put(id, new Document(id, id.toUpperCase(), "The " + id + ".", 1, SEEDED));
        }
    }

    public Document get(String id) {
        Document document = documents.get(id);
        if (document == null) {
            throw new DocumentNotFoundException(id);
        }
        return document;
    }

    public Document create(DocumentRequest request) {
        String id = "doc-" + nextId.getAndIncrement();
        Document document = new Document(id, request.title(), request.body(), 1, now(null));
        documents.put(id, document);
        return document;
    }

    public synchronized Document replace(String id, DocumentRequest request) {
        Document old = get(id);
        Document changed = new Document(id, request.title(), request.body(), old.version() + 1, now(old.updatedAt()));
        documents.put(id, changed);
        return changed;
    }

    public void delete(String id) {
        if (documents.remove(id) == null) {
            throw new DocumentNotFoundException(id);
        }
    }

    private static Instant now(Instant previous) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return previous != null && !now.isAfter(previous) ? previous.plusSeconds(1) : now;
    }
}
