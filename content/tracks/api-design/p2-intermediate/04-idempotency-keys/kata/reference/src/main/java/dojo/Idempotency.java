package dojo;

import dojo.IdempotencyExceptions.KeyMissingException;
import dojo.IdempotencyExceptions.KeyReusedException;
import dojo.IdempotencyExceptions.RequestInProgressException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Makes a non-idempotent operation safe to retry: the first request with a key runs, later
 * requests with the same key get the first answer again.
 */
@Component
public class Idempotency {
    static final Duration KEPT_FOR = Duration.ofHours(24);
    static final String REPLAYED = "Idempotent-Replayed";

    /** What the first request answered. */
    private record Stored(HttpStatusCode status, HttpHeaders headers, Object body) {}

    /** One key: its request fingerprint, when it was first used, and its answer once done. */
    private record Slot(String fingerprint, Instant firstUsed, Stored answer) {
        boolean expired(Instant now) {
            return !now.isBefore(firstUsed.plus(KEPT_FOR));
        }

        Slot done(ResponseEntity<?> response) {
            HttpHeaders headers = new HttpHeaders();
            headers.addAll(response.getHeaders());
            return new Slot(fingerprint, firstUsed, new Stored(response.getStatusCode(), headers, response.getBody()));
        }
    }

    private final Ticker ticker;
    private final Map<String, Slot> slots = new ConcurrentHashMap<>();

    public Idempotency(Ticker ticker) {
        this.ticker = ticker;
    }

    /**
     * Runs {@code action} at most once per client and key.
     *
     * @param client    who is calling (the X-Api-Key header): keys of different clients never clash
     * @param key       the Idempotency-Key header, or null when the client sent none
     * @param operation the method and path, for example "POST /payments"
     * @param request   the request body; records compare by value
     * @param action    the operation itself
     */
    public ResponseEntity<?> execute(String client, String key, String operation, Object request,
            Supplier<ResponseEntity<?>> action) {
        if (key == null || key.isBlank()) {
            throw new KeyMissingException();
        }
        String id = client + "\n" + key;
        String fingerprint = operation + "\n" + request;
        Instant now = ticker.now();
        Slot mine = new Slot(fingerprint, now, null);
        while (true) {
            Slot existing = slots.putIfAbsent(id, mine);
            if (existing == null) {
                return run(id, mine, action);
            }
            if (existing.expired(now)) {
                slots.remove(id, existing);
                continue;
            }
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new KeyReusedException(key);
            }
            if (existing.answer() == null) {
                throw new RequestInProgressException(key);
            }
            return replay(existing.answer());
        }
    }

    private ResponseEntity<?> run(String id, Slot mine, Supplier<ResponseEntity<?>> action) {
        ResponseEntity<?> response;
        try {
            response = action.get();
        } catch (RuntimeException e) {
            slots.remove(id, mine); // nothing happened for sure: let the client retry
            throw e;
        }
        if (response.getStatusCode().is5xxServerError()) {
            slots.remove(id, mine);
        } else {
            slots.replace(id, mine, mine.done(response));
        }
        return response;
    }

    private static ResponseEntity<?> replay(Stored answer) {
        return ResponseEntity.status(answer.status())
                .headers(answer.headers())
                .header(REPLAYED, "true")
                .body(answer.body());
    }
}
