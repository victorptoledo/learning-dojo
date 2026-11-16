package dojo;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /events, newest first. Today it pages with an offset, which skips and repeats events
 * whenever new ones arrive between two requests.
 */
@RestController
public class FeedController {
    private final EventStore store;

    public FeedController(EventStore store) {
        this.store = store;
    }

    @GetMapping("/events")
    public List<Event> events(@RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        return store.all().reversed().stream().skip(offset).limit(limit).toList();
    }
}
