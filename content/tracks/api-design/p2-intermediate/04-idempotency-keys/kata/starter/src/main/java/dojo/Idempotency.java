package dojo;

import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Makes a non-idempotent operation safe to retry: the first request with a key runs, later
 * requests with the same key get the first answer again. Today it does nothing.
 */
@Component
public class Idempotency {
    private final Ticker ticker;

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
        return action.get();
    }
}
