package dojo;

import dojo.DocumentExceptions.PreconditionFailedException;
import dojo.DocumentExceptions.PreconditionRequiredException;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The documents API with validators: ETag and Last-Modified, 304 on reads, If-Match on writes. */
@RestController
@RequestMapping("/documents")
public class DocumentController {
    private static final CacheControl REVALIDATE = CacheControl.noCache().cachePrivate();

    private final DocumentStore store;

    public DocumentController(DocumentStore store) {
        this.store = store;
    }

    /** A strong ETag: the version, quoted. */
    static String etag(Document document) {
        return "\"" + document.version() + "\"";
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> get(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        Document document = store.get(id);
        String etag = etag(document);
        List<String> ifNoneMatch = headers.getIfNoneMatch();
        boolean notModified;
        if (!ifNoneMatch.isEmpty()) {
            // If-None-Match wins over If-Modified-Since, and compares weakly.
            notModified = ifNoneMatch.stream().anyMatch(tag -> tag.equals("*") || weak(tag).equals(weak(etag)));
        } else {
            long since = headers.getIfModifiedSince();
            notModified = since >= 0 && !document.updatedAt().isAfter(Instant.ofEpochMilli(since));
        }
        if (notModified) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .lastModified(document.updatedAt())
                    .cacheControl(REVALIDATE)
                    .build();
        }
        return withValidators(ResponseEntity.ok(), document).body(document);
    }

    @PostMapping
    public ResponseEntity<Document> create(@Valid @RequestBody DocumentRequest request) {
        Document document = store.create(request);
        return withValidators(ResponseEntity.created(URI.create("/documents/" + document.id())), document)
                .body(document);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> replace(@PathVariable String id, @RequestHeader HttpHeaders headers,
            @Valid @RequestBody DocumentRequest request) {
        requireMatch(id, headers);
        Document changed = store.replace(id, request);
        return withValidators(ResponseEntity.ok(), changed).body(changed);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @RequestHeader HttpHeaders headers) {
        requireMatch(id, headers);
        store.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** If-Match compares strongly: a weak ETag never matches. */
    private void requireMatch(String id, HttpHeaders headers) {
        String current = etag(store.get(id));
        List<String> ifMatch = headers.getIfMatch();
        if (ifMatch.isEmpty()) {
            throw new PreconditionRequiredException();
        }
        if (ifMatch.stream().noneMatch(tag -> tag.equals("*") || tag.equals(current))) {
            throw new PreconditionFailedException(id);
        }
    }

    private static ResponseEntity.BodyBuilder withValidators(ResponseEntity.BodyBuilder builder, Document document) {
        return builder.eTag(etag(document)).lastModified(document.updatedAt()).cacheControl(REVALIDATE);
    }

    private static String weak(String tag) {
        return tag.startsWith("W/") ? tag.substring(2) : tag;
    }
}
