package dojo;

import dojo.FeedExceptions.InvalidParameterException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** GET /events, newest first, paged with an opaque keyset cursor. */
@RestController
public class FeedController {
    static final Comparator<Event> NEWEST_FIRST = Comparator.comparing(Event::occurredAt)
            .thenComparingLong(Event::id)
            .reversed();

    private final EventStore store;

    public FeedController(EventStore store) {
        this.store = store;
    }

    @GetMapping("/events")
    public ResponseEntity<FeedPage> events(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > 100) {
            throw new InvalidParameterException("limit", "limit must be between 1 and 100");
        }
        Cursor after = cursor == null ? null : Cursor.decode(cursor, type);
        // One more than asked tells whether another page exists.
        List<Event> window = store.all().stream()
                .filter(event -> type == null || event.type().equals(type))
                .filter(event -> after == null || after.isBefore(event))
                .sorted(NEWEST_FIRST)
                .limit(limit + 1L)
                .toList();
        boolean more = window.size() > limit;
        List<Event> data = more ? window.subList(0, limit) : window;
        String next = more ? Cursor.after(data.getLast(), type).encode() : null;
        ResponseEntity.BodyBuilder answer = ResponseEntity.ok();
        if (next != null) {
            String url = ServletUriComponentsBuilder.fromCurrentRequest()
                    .replaceQueryParam("cursor", next)
                    .toUriString();
            answer.header(HttpHeaders.LINK, "<" + url + ">; rel=\"next\"");
        }
        return answer.body(new FeedPage(data, next));
    }
}
