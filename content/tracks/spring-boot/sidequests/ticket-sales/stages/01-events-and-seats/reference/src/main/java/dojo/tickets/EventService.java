package dojo.tickets;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
    private final EventRepository events;
    private final SeatRepository seats;
    private final Clock clock;

    public EventService(EventRepository events, SeatRepository seats, Clock clock) {
        this.events = events;
        this.seats = seats;
        this.clock = clock;
    }

    /** The event and every one of its seats, in one transaction. */
    @Transactional
    public Event create(NewEvent request) {
        if (!request.startsAt().isAfter(clock.instant())) {
            throw new RuleException("an event must start in the future");
        }
        Event event = events.save(new Event(request.name().strip(), request.startsAt(), request.priceCents(),
                request.rows() * request.seatsPerRow()));
        List<Seat> all = new ArrayList<>();
        for (int row = 0; row < request.rows(); row++) {
            for (int number = 1; number <= request.seatsPerRow(); number++) {
                all.add(new Seat(event, row, number));
            }
        }
        seats.saveAll(all);
        return event;
    }

    @Transactional(readOnly = true)
    public Event get(long id) {
        return events.findById(id).orElseThrow(() -> new NotFoundException("event " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public EventView view(long id) {
        return EventView.of(get(id), seats.countByEventIdAndStatus(id, SeatStatus.AVAILABLE));
    }

    @Transactional(readOnly = true)
    public List<SeatView> seats(long id) {
        get(id);
        return seats.findByEventIdOrderByRowIndexAscNumberAsc(id).stream()
                .map(seat -> new SeatView(seat.getLabel(), seat.getStatus()))
                .toList();
    }
}
