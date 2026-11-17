package dojo.tickets;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/events")
public class EventController {
    private final EventService events;

    public EventController(EventService events) {
        this.events = events;
    }

    @PostMapping
    public ResponseEntity<EventView> create(@Valid @RequestBody NewEvent request, UriComponentsBuilder uri) {
        Event event = events.create(request);
        return ResponseEntity.created(uri.path("/events/{id}").build(event.getId())).body(events.view(event.getId()));
    }

    @GetMapping("/{id}")
    public EventView get(@PathVariable long id) {
        return events.view(id);
    }

    @GetMapping("/{id}/seats")
    public List<SeatView> seats(@PathVariable long id) {
        return events.seats(id);
    }
}
